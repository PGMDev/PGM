package tc.oc.pgm.payload;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Minecart;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.goals.ControllableGoal;
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.regions.CylindricalRegion;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.text.TextFormatter;

// ---------------------------Terminology-----------------------------
// -- Rail -> A collection of nested Paths(Stored in each other)     I
// --                                                                I
// -- Path -> A point on the rail.                                   I
// --         When the payload moves it moves from path to path.     I
// --         This does not mean that each single possible point     I
// --         the payload can exist on is a Path, but rather that    I
// --         the movement of the payload is always from one Path    I
// --         to another.                                            I
// --                                                                I
// --                                                                I
// -- Head/Tail -> Head is towards a higher index on the rail(->max) I
// --              tail is towards a lower index on the rail(->0)    I
// -------------------------------------------------------------------

public class Payload extends ControllableGoal<PayloadDefinition>
    implements IncrementalGoal<PayloadDefinition> {

  // The team that currently owns the payload
  // Teams that can not push the payload CAN be the owner
  // Is null if no players nearby
  @Nullable private Competitor currentOwner = null;

  // The amount of Paths in this payloads rail
  private int railSize;

  private Minecart payloadEntity;
  private ArmorStand labelEntity;

  // The final path in their respective direction
  private Path headPath;
  private Path tailPath;

  // The current whereabouts of the payload
  private Path currentPath;
  private Location payloadLocation;

  // The path the payload goes towards in the neutral state, the path the payload starts on;
  private Path middlePath;

  // Two paths used to store furthest progression in each direction
  private Path furthestHeadPath;
  private Path furthestTailPath;

  // Switches to true when the payload reaches a end
  private boolean completed = false;

  private final Map<Integer, PayloadCheckpoint> checkpointMap = new HashMap<>();
  private int lastReachedCheckpointKey = 0; // 0 means middle
  private int headCheckpointsAmount;
  private int tailCheckpointsAmount;

  private final TeamMatchModule tmm;
  private Competitor leadingTeam;

  public static final ChatColor COLOR_NEUTRAL_TEAM = ChatColor.WHITE;

  public static final String SYMBOL_PAYLOAD_NEUTRAL = "\u29be"; // ⦾

  /** The default {@link MaterialMatcher} used for checking if a payload is on a checkpoint */
  public static final MaterialMatcher STANDARD_CHECKPOINT_MATERIALS =
      new CompoundMaterialMatcher(
          ImmutableList.of(
              new SingleMaterialMatcher(Material.DETECTOR_RAIL),
              new SingleMaterialMatcher(Material.ACTIVATOR_RAIL),
              new SingleMaterialMatcher(Material.POWERED_RAIL)));

  public Payload(Match match, PayloadDefinition definition) {
    super(definition, match);

    tmm = match.needModule(TeamMatchModule.class);

    createPayload();
  }

  // Incrementable

  @Override // Returns the leading completion as a number between 0 and 1
  public double getCompletion() {
    return getCompletion(leadingTeam);
  }

  @Override
  public String renderCompletion() {
    return (currentPath.isCheckpoint()
        ? SYMBOL_PAYLOAD_NEUTRAL
        : (pathsUntilNextCheckpoint() + "m"));
  }

  public @Nullable String renderPreciseCompletion() {
    return null;
  }

  public boolean getShowProgress() {
    return definition.shouldShowProgress();
  }

  public double getCompletion(Competitor competitor) {

    // The total amount of rails from the middle to the relevant goal
    int total = 0;
    // The amount of rails this team has progressed from the middle towards the relevant goal
    int progress = 0;

    if (tmm.getTeam(definition.getPrimaryOwner()) == competitor) {
      total = railSize - middlePath.index();
      progress = (furthestHeadPath.index() - middlePath.index());

    } else if (definition.getSecondaryOwner() != null
        && tmm.getTeam(definition.getSecondaryOwner()) == competitor) {
      total = middlePath.index();
      progress = middlePath.index() - furthestTailPath.index();
    }

    if (total == 0 || progress == 0) return 0;

    return progress / (double) total;
  }

  /**
   * Gets the remaining amount of paths until the next checkpoint
   *
   * @return the remaining amount of paths until the next checkpoint
   */
  public int pathsUntilNextCheckpoint() {
    if (currentPath.index() == middlePath.index() || currentPath.isCheckpoint()) return 0;
    return Math.abs(
        currentPath.index() - getNextCheckpoint(currentPath.index() > middlePath.index()).index());
  }

  //

  @Override
  public boolean canComplete(Competitor team) {
    if (team instanceof Team)
      return ((Team) team).isInstance(definition.getPrimaryOwner())
          || (((Team) team).isInstance(definition.getSecondaryOwner())
              && !definition
                  .shouldSecondaryTeamPushButNoGoal()); // If they have no goal they cant complete
    // anything
    return false;
  }

  @Override
  public boolean isCompleted() {
    return completed;
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return isCompleted() && currentOwner != null && currentOwner.equals(team);
  }

  @Override
  public boolean isShared() {
    return true;
  }

  @Override
  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return ChatColor.WHITE;
  }

  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return renderCompletion();
  }

  public ChatColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
    return getControllingTeamColor();
  }

  // Controllable

  @Override // Called in ControllableGoal#contestCycle
  public void dominationCycle(@Nullable Competitor dominatingTeam, int lead, Duration duration) {
    currentOwner = dominatingTeam; // The team that completes the CaptureCondition
  }

  // Ticktickticktick
  public void tick() {

    if (isCompleted()) return; // Freeze if completed

    // Move if conditions are present
    tickMove();

    // Display the pretty circle around the payload
    tickDisplay();

    // Check if any team has gotten further than their last record
    // Returns true if an updated completion is further than the last updated
    // completion for the opposite side
    if (updateCompletion()) leadingTeam = currentOwner;

    match.callEvent(new GoalStatusChangeEvent(match, this));
  }

  private void tickMove() {

    float speed = getControllingTeamSpeed(); // Fetches the speed for the current owner

    final float points = definition.getPoints();

    // Check if the payload has reached a finish
    if ((isUnderPrimaryOwnerControl() ? !currentPath.hasNext() : !currentPath.hasPrevious())
        && !(!isUnderPrimaryOwnerControl() && definition.shouldSecondaryTeamPushButNoGoal())) {
      completed = true;
      match.callEvent(new GoalCompleteEvent(match, this, currentOwner, true));

      final ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
      if (smm != null) { // Increment points if the score module is present(default points is 0)
        if (currentOwner != null) smm.incrementScore(currentOwner, points);
      }

      return; // Dont execute a moving cycle if the payload has been completed
    }

    speed = Math.abs(speed);
    if (speed == 0) return; // Mainly for if neutral speed is 0

    move(speed / 10.0); // Move the entity
  }

  private void move(double distance) {
    if (!currentPath.hasNext() || !currentPath.hasPrevious()) { // Path is over
      payloadEntity.teleport(currentPath.getLocation()); // Teleport to final location
      return;
    }

    // Dont try to do neutral moving if the payload has reached the middle
    if (currentPath.index() == middlePath.index() && isNeutral()) return;

    // If the current Path is a checkpoint
    if (currentPath.isCheckpoint()) {
      // AND it is not the same checkpoint as the last one reached
      if (currentPath.index() != getLastReachedCheckpoint().index())
        // then refresh the lastReachedCheckpointKey
        calculateCheckpointContext(true);

      // If its permanent and trying to move the wrong way..
      // Stop it!!
      if (hasPayloadHitPermanentCheckpoint()) return;
    }

    // If the current path is closer to the middle than the last reached checkpoint
    // AND the last reached checkpoint is not the middle
    if (!getLastReachedCheckpoint().isMiddle()
        && Math.abs(currentPath.index() - middlePath.index())
            < Math.abs(getLastReachedCheckpoint().index() - middlePath.index())) {
      // then refresh the lastReachedCheckpointKey
      calculateCheckpointContext(false);
    }

    Path nextPath = getControllingTeamPath();
    Vector direction = nextPath.getLocation().toVector().subtract(payloadLocation.toVector());
    double len = direction.length(), extraLen = distance - len;
    // If there's extra distance, skip calculations, otherwise, move payload proportionally
    if (extraLen > 0) {
      currentPath = nextPath;
      move(extraLen);
    } else payloadLocation.add(direction.multiply(distance / len));

    // Actually move the payload
    payloadEntity.teleport(payloadLocation);

    // Also remember to move the label ArmorStand
    Location labelLocation = payloadEntity.getLocation().clone();
    labelLocation.setY(labelLocation.getY() - 0.2);
    labelEntity.teleport(labelLocation);

    // Tell the tracker that the region has moved
    refreshRegion();
  }

  private void calculateCheckpointContext(boolean forwards) {
    PayloadCheckpoint newCheckpoint =
        forwards
            ?
            // A new checkpoint further away from the middle
            getNextCheckpoint(currentPath.index() > middlePath.index())
            :
            // A new checkpoint closer to the middle(Triggered when a payload goes back over a
            // previously
            // passed checkpoint)
            getNextCheckpoint(lastReachedCheckpointKey < 0);

    if (getLastReachedCheckpoint() != newCheckpoint) {

      int oldCheckpointKey = lastReachedCheckpointKey;

      // First call the event
      match.callEvent(new PayloadReachCheckpointEvent(this, currentOwner, forwards));
      // Then actually change the last reached checkpoint
      lastReachedCheckpointKey = newCheckpoint.getMapIndex();

      int relevantCheckpointKey = forwards ? lastReachedCheckpointKey : oldCheckpointKey;

      TextColor checkpointColor = TextColor.WHITE;
      if (relevantCheckpointKey != 0)
        checkpointColor =
            TextFormatter.convert(
                (relevantCheckpointKey > 0
                    ? tmm.getTeam(definition.getPrimaryOwner()).getColor()
                    // If any tail checkpoints exist, then the secondary team should NOT be null
                    : tmm.getTeam(definition.getSecondaryOwner()).getColor()));

      final Component message =
          TranslatableComponent.of(
              forwards
                  ? "payload.reachedCheckpoint.forwards"
                  : "payload.reachedCheckpoint.backwards",
              this.getComponentName().color(TextFormatter.convert(getControllingTeamColor())),
              TextComponent.of(Math.abs(relevantCheckpointKey), checkpointColor),
              TextComponent.of(
                  relevantCheckpointKey > 0 ? headCheckpointsAmount : tailCheckpointsAmount,
                  checkpointColor));
      match.sendMessage(message);
    }
  }

  /** Gets the checkpoint furthest away from the middle currently reached */
  public final PayloadCheckpoint getLastReachedCheckpoint() {
    return checkpointMap.get(lastReachedCheckpointKey);
  }

  /**
   * Gets the next {@link PayloadCheckpoint} in the given direction
   *
   * @param head {@code true} if the next checkpoint in the head direction should be returned {@code
   *     false} if the next checkpoint in the tail direction should be returned}
   */
  private PayloadCheckpoint getNextCheckpoint(boolean head) {
    return checkpointMap.get(lastReachedCheckpointKey - (head ? -1 : 1));
  }

  private boolean hasPayloadHitPermanentCheckpoint() {
    if (!getLastReachedCheckpoint().isMiddle()) {

      return ((isUnderPrimaryOwnerControl() || isNeutral())
              && getLastReachedCheckpoint().index() < middlePath.index()
              && getLastReachedCheckpoint().isPermanent())
          || ((isUnderSecondaryOwnerControl() || isNeutral())
              && getLastReachedCheckpoint().index() > middlePath.index()
              && getLastReachedCheckpoint().isPermanent());
    }
    return false; // No checkpoint has been hit yet
  }

  private void tickDisplay() {
    Color color = isNeutral() ? Color.WHITE : currentOwner.getFullColor();
    // Each iteration of this loop is one particle
    for (double angle = 0; angle <= 360; angle += 3.6) {
      Location particle =
          payloadLocation
              .clone()
              .add(
                  new Vector(
                      definition.getRadius() * Math.cos(angle),
                      0.5,
                      definition.getRadius() * Math.sin(angle)));
      match
          .getWorld()
          .spigot()
          .playEffect(
              particle,
              Effect.COLOURED_DUST,
              0,
              (byte) 0,
              rgbToParticle(color.getRed()),
              rgbToParticle(color.getGreen()),
              rgbToParticle(color.getBlue()),
              1,
              0,
              200);
    }

    // Set the wool block inside the payload to the color of the controlling team
    byte blockData = BukkitUtils.chatColorToDyeColor(getControllingTeamColor()).getWoolData();
    MaterialData data = payloadEntity.getDisplayBlock();
    data.setData(blockData);
    payloadEntity.setDisplayBlock(data);
  }

  private float rgbToParticle(int rgb) {
    return (float) Math.max(0.001, rgb / 255.0);
  }

  private boolean updateCompletion() {
    if (currentPath.index() > middlePath.index()) {
      if (furthestHeadPath == null || currentPath.index() > furthestHeadPath.index()) {
        furthestHeadPath = currentPath;
        return furthestHeadPath.index() - middlePath.index()
            > middlePath.index() - furthestTailPath.index();
      }
    } else if (furthestTailPath == null || currentPath.index() < furthestTailPath.index()) {
      furthestTailPath = currentPath;
      return furthestHeadPath.index() - middlePath.index()
          < middlePath.index() - furthestTailPath.index();
    }

    return false;
  }

  public void createPayload() {
    // Order is important!

    makeRail();

    summonMinecart();

    refreshRegion();
  }

  private void summonMinecart() {
    Location minecartSpawn =
        middlePath.getLocation().clone().toCenterLocation().subtract(0, 0.5, 0);

    payloadLocation = minecartSpawn;

    // Set the floor to gold
    Location below = minecartSpawn.clone();
    below.setY(minecartSpawn.getY() - 1);
    below.getBlock().setType(Material.GOLD_BLOCK);

    // Spawn the Payload entity
    Location spawn = minecartSpawn.clone();
    payloadEntity = minecartSpawn.getWorld().spawn(spawn, Minecart.class);
    MaterialData payloadBlock = new MaterialData(Material.WOOL);
    payloadBlock.setData((byte) 0);
    payloadEntity.setDisplayBlock(payloadBlock);
    payloadEntity.setMaxSpeed(0);
    payloadEntity.setSlowWhenEmpty(true);

    // Summon a label for it
    labelEntity =
        payloadLocation.getWorld().spawn(payloadLocation.clone().add(0, 0.2, 0), ArmorStand.class);
    labelEntity.setVisible(false);
    labelEntity.setGravity(false);
    labelEntity.setRemoveWhenFarAway(false);
    labelEntity.setArms(false);
    labelEntity.setBasePlate(false);
    labelEntity.setCustomName(getColoredName());
    labelEntity.setCustomNameVisible(true);
    labelEntity.setCanPickupItems(false);
  }

  private void refreshRegion() {
    setControllableRegion(
        new CylindricalRegion(
            payloadLocation.toVector(), definition.getRadius(), definition.getHeight()));
  }

  private Path getControllingTeamPath() {

    if (canDominate(currentOwner)) {
      if (isUnderPrimaryOwnerControl()) return currentPath.next();
      if (isUnderSecondaryOwnerControl()) return currentPath.previous();
    }

    return (currentPath.index() < middlePath.index()) // Payload moves towards middle when neutral
        ? currentPath.next()
        : currentPath.previous();
  }

  /** Gets the relevant speed value from the {@link Payload}s {@link PayloadDefinition} */
  private float getControllingTeamSpeed() {
    if (canDominate(currentOwner)) {
      if (isUnderPrimaryOwnerControl()) return definition.getPrimaryOwnerSpeed();
      if (isUnderSecondaryOwnerControl()) return definition.getSecondaryOwnerSpeed();
    }

    // If any non-pushing team controls the payload, neutral speed should be used
    return definition.getNeutralSpeed();
  }

  private ChatColor getControllingTeamColor() {
    return isNeutral() ? COLOR_NEUTRAL_TEAM : currentOwner.getColor();
  }

  private boolean isUnderPrimaryOwnerControl() {
    return currentOwner == tmm.getTeam(definition.getPrimaryOwner());
  }

  private boolean isUnderSecondaryOwnerControl() {
    // Ensure that if there is no secondary owner (definition.getCompetingOwner = null)
    // this does not return true if the state is neutral (currentOwner = null)
    if (definition.getSecondaryOwner() != null)
      return currentOwner == tmm.getTeam(definition.getSecondaryOwner());
    return false;
  }

  private boolean isNeutral() {
    return currentOwner == null;
  }

  protected void makeRail() {
    final Location location = definition.getStartingLocation().toLocation(getMatch().getWorld());

    if (!isValidStartLocation(location)) return;

    final Rails startingRails = (Rails) location.getBlock().getState().getMaterialData();

    BlockFace direction = startingRails.getDirection();

    final List<Double> differingX = new ArrayList<>();
    final List<Double> differingY = new ArrayList<>();
    final List<Double> differingZ = new ArrayList<>();

    differingY.add(0.0);
    differingY.add(1.0);
    differingY.add(-1.0);

    headPath = new Path(railSize, location, null, null);
    railSize++;

    Path previousPath = headPath;
    Path neighborRail =
        getNewNeighborPath(previousPath, direction, differingX, differingY, differingZ);

    while (neighborRail != null) {

      previousPath.setNext(neighborRail);

      previousPath = neighborRail;

      differingX.clear();
      differingZ.clear();

      if (previousPath.getLocation().getBlock().getState().getMaterialData() instanceof Rails) {
        direction =
            ((Rails) previousPath.getLocation().getBlock().getState().getMaterialData())
                .getDirection();
      } else {
        direction = null;
      }

      neighborRail =
          getNewNeighborPath(previousPath, direction, differingX, differingY, differingZ);
    }

    tailPath = previousPath;

    Path currentPath = headPath;
    Path lastPath = null;

    headPath = null;

    boolean moreRails = currentPath.hasNext();
    while (moreRails) {

      Path nextPath = currentPath.next();
      Location newLocation =
          currentPath
              .getLocation()
              .toVector()
              .midpoint(nextPath.getLocation().toVector())
              .toLocation(getMatch().getWorld());
      newLocation.setY(Math.max(currentPath.getLocation().getY(), nextPath.getLocation().getY()));
      Path newPath;
      if (headPath == null) {
        Location headLocation =
            newLocation.clone().add(currentPath.getLocation().subtract(nextPath.getLocation()));
        headPath = new Path(railSize, headLocation, null, null);
        railSize++;
        newPath = new Path(railSize, newLocation, headPath, null);
        railSize++;
        headPath.setNext(newPath);
        lastPath = newPath;
        this.currentPath = headPath;
      } else {
        newPath = new Path(railSize, newLocation, lastPath, null, currentPath.isCheckpoint());
        railSize++;
        lastPath.setNext(newPath);
        lastPath = newPath;
        tailPath = lastPath;
      }

      currentPath = nextPath;
      moreRails = currentPath.hasNext();
    }

    Path tail = tailPath;
    Path beforeTail = tail.previous();
    Location newLocation =
        tail.getLocation()
            .toVector()
            .midpoint(beforeTail.getLocation().toVector())
            .toLocation(getMatch().getWorld());
    newLocation.setY(Math.max(tail.getLocation().getY(), beforeTail.getLocation().getY()));
    Location tailLocation =
        newLocation.clone().add(currentPath.getLocation().subtract(beforeTail.getLocation()));
    tailPath = new Path(tailLocation, tail, null);
    tail.setNext(tailPath);

    Location lookingFor = definition.getMiddleLocation().toLocation(match.getWorld());

    middlePath = findPath(tail, lookingFor);

    if (middlePath == null) match.getLogger().warning("No middle path found");

    this.currentPath = middlePath;

    furthestTailPath = middlePath;
    furthestHeadPath = middlePath;

    makeCheckpoints();
  }

  private void makeCheckpoints() {
    // Checkpoint calculation

    final List<Integer> permanentHead = definition.getPermanentHeadCheckpoints();
    final List<Integer> permanentTail = definition.getPermanentTailCheckpoints();

    Path discoverCheckpoints = middlePath;
    int h = 1; // 0 is reserved for middle
    while (discoverCheckpoints.hasNext()) {
      Path potentialCheckpoint =
          discoverCheckpoints.next(); // No reason to check for a checkpoint ON the middle path
      if (potentialCheckpoint.isCheckpoint()) {
        int index = potentialCheckpoint.index();
        boolean permanent = false;
        if (permanentHead != null) permanent = permanentHead.contains(checkpointMap.size() + 1);
        checkpointMap.put(h, new PayloadCheckpoint(index, h, permanent));
        headCheckpointsAmount++;
        h++;
      }
      discoverCheckpoints = discoverCheckpoints.next();
    }

    // Adding the end in this direction as a checkpoint
    checkpointMap.put(h, new PayloadCheckpoint(discoverCheckpoints.index(), h, false));

    discoverCheckpoints = middlePath; // Reset

    final int offset = checkpointMap.size();

    int t = -1;
    while (discoverCheckpoints.hasPrevious()) {
      Path potentialCheckpoint = discoverCheckpoints.previous();
      if (potentialCheckpoint.isCheckpoint()) {
        int index = potentialCheckpoint.index();
        boolean permanent = false;
        if (permanentTail != null)
          permanent = permanentTail.contains(checkpointMap.size() - offset + 1);
        checkpointMap.put(t, new PayloadCheckpoint(index, t, permanent));
        tailCheckpointsAmount++;
        t--;
      }
      discoverCheckpoints = discoverCheckpoints.previous();
    }

    // Adding the end in this direction as a checkpoint
    checkpointMap.put(t, new PayloadCheckpoint(discoverCheckpoints.index(), t, false));

    // Adding the middle as a checkpoint
    checkpointMap.put(0, new PayloadCheckpoint(middlePath.index(), 0, false));
  }

  /** Checks if a {@link Material} is any type of rail(normal, detector, activator, powered) */
  public boolean isRails(Material material) {
    // STANDARD_CHECKPOINT_MATERIALS covers all other types of rails
    // The fact that both isRails AND isCheckpoint can be true for a single Path is OK
    return material.equals(Material.RAILS) || STANDARD_CHECKPOINT_MATERIALS.matches(material);
  }

  private boolean isValidStartLocation(Location location) {
    // Payload must start on a rail
    if (!isRails(location.getBlock().getType())) {
      getMatch().getLogger().warning("No rail found in starting position for payload");
      return false;
    }

    final Rails startingRails = (Rails) location.getBlock().getState().getMaterialData();

    if (startingRails.isCurve() || startingRails.isOnSlope()) {
      getMatch().getLogger().warning("Starting rail can not be either curved or in a slope");
      return false;
    }

    if (isCheckpoint(location.getBlock())) {
      getMatch().getLogger().warning("Starting rail can not be a checkpoint");
      return false;
    }
    return true;
  }

  /**
   * Checks the given {@link Block} and the block below({@link BlockFace#DOWN}) for any {@link
   * Material}s matching with either a xml-defined {@link MaterialMatcher} or {@link
   * Payload#STANDARD_CHECKPOINT_MATERIALS} by default
   */
  private boolean isCheckpoint(Block block) {
    final MaterialMatcher materialMatcher;

    if (definition.getCheckpointMaterial() != null)
      materialMatcher = definition.getCheckpointMaterial();
    else materialMatcher = STANDARD_CHECKPOINT_MATERIALS;

    return materialMatcher.matches(block.getType())
        || materialMatcher.matches(block.getRelative(BlockFace.DOWN).getType());
  }

  /**
   * Looks along this {@link Payload}s rail for a {@link Path} that has a specific {@link Location}
   *
   * @param start The starting point for this algorithm
   * @param lookingFor The point on this rail to look for
   * @return A {@link Path}, or {@code null} if no {@link Path} with the given {@link Location}
   *     exsists on this rail
   */
  @Nullable
  private Path findPath(Path start, Location lookingFor) {
    Path result = null;
    Path backwards = start;
    while (backwards.hasPrevious()) { // Look behind and onwards
      Location here = backwards.getLocation().toCenterLocation();
      if (here.equals(lookingFor.toCenterLocation())) {
        result = backwards;
        break;
      }
      backwards = backwards.previous();
    }
    if (result == null) { // If not yet found, try looking the other way
      Path forwards = start;
      while (forwards.hasNext()) {
        Location here = forwards.getLocation().toCenterLocation();
        if (here.equals(lookingFor.toCenterLocation())) {
          result = forwards;
          break;
        }
        forwards = forwards.previous();
      }
    }
    return result;
  }

  private Path getNewNeighborPath(
      Path path,
      BlockFace direction,
      List<Double> differingX,
      List<Double> differingY,
      List<Double> differingZ) {
    Location previousLocation = null;
    if (path.previous() != null) {
      previousLocation = path.previous().getLocation();
    }

    Location location = path.getLocation();

    if (direction == null) {
      differingX.add(-1.0);
      differingX.add(0.0);
      differingX.add(1.0);
      differingZ.add(-1.0);
      differingZ.add(0.0);
      differingZ.add(1.0);
    } else if (direction.equals(BlockFace.SOUTH) || direction.equals(BlockFace.NORTH)) {
      differingZ.add(-1.0);
      differingZ.add(1.0);
      differingX.add(0.0);
    } else if (direction.equals(BlockFace.EAST) || direction.equals(BlockFace.WEST)) {
      differingX.add(-1.0);
      differingX.add(1.0);
      differingZ.add(0.0);
    } else {
      Location side = location.clone();
      side.setZ(
          side.getZ()
              + (direction.equals(BlockFace.NORTH_WEST) || direction.equals(BlockFace.NORTH_EAST)
                  ? 1
                  : -1));
      if (side.getX() == previousLocation.getX() && side.getZ() == previousLocation.getZ()) {
        differingX.add(
            direction.equals(BlockFace.SOUTH_WEST) || direction.equals(BlockFace.NORTH_WEST)
                ? 1.0
                : -1.0);
        differingZ.add(0.0);
      } else {
        differingX.add(0.0);
        differingZ.add(
            direction.equals(BlockFace.NORTH_WEST) || direction.equals(BlockFace.NORTH_EAST)
                ? 1.0
                : -1.0);
      }
    }

    Location newLocation = location.clone();
    for (double x : differingX) {
      for (double y : differingY) {
        for (double z : differingZ) {
          newLocation.add(x, y, z);

          boolean isCheckpoint = isCheckpoint(newLocation.getBlock());

          if (isRails(newLocation.getBlock().getType()) || isCheckpoint) {
            Path currentPath = path;
            if (currentPath.equals(headPath)) {
              return new Path(newLocation, path, null, isCheckpoint);
            }

            boolean alreadyExists = false;
            while (currentPath.hasPrevious()) {
              if (currentPath.getLocation().toVector().equals(newLocation.toVector())) {
                alreadyExists = true;
                break;
              }
              currentPath = currentPath.previous();
            }

            if (!alreadyExists) {
              return new Path(newLocation, path, null, isCheckpoint);
            }
          }

          newLocation.subtract(x, y, z);
        }
      }
    }
    return null;
  }

  /** Checks if the given {@link Minecart} is the payload minecart */
  public boolean isPayload(Minecart minecart) {
    return minecart == payloadEntity;
  }
}
