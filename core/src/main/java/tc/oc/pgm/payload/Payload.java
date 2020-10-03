package tc.oc.pgm.payload;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.*;
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
  // Is null if no players nearby (Neutral state)
  @Nullable private Competitor currentOwner = null;

  // The amount of Paths in this payloads rail
  private int railSize;

  private Minecart payloadEntity;
  private ArmorStand labelEntity;

  // The current whereabouts of the payload
  private Path currentPath;
  private Location payloadLocation;

  // The path the payload goes towards in the neutral state, the path the payload starts on;
  private Path middlePath;

  private Competitor primaryOwner;
  private Path primaryGoal;

  @Nullable private Competitor secondaryOwner;
  @Nullable private Path secondaryGoal;

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

  public static final MaterialMatcher RAIL_MATERIAL_MATCHER =
      new CompoundMaterialMatcher(
          ImmutableList.of(
              new SingleMaterialMatcher(Material.DETECTOR_RAIL),
              new SingleMaterialMatcher(Material.ACTIVATOR_RAIL),
              new SingleMaterialMatcher(Material.POWERED_RAIL),
              new SingleMaterialMatcher(Material.RAILS)));

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

  @Override
  public double getCompletion(Competitor competitor) {

    // The total amount of rails from the middle to the relevant goal
    int total = 0;
    // The amount of rails this team has progressed from the middle towards the relevant goal
    int progress = 0;

    if (primaryOwner == competitor) {
      total = railSize - middlePath.index();
      progress = (furthestHeadPath.index() - middlePath.index());

    } else if (secondaryOwner != null && secondaryOwner == competitor) {
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
  private int pathsUntilNextCheckpoint() {
    if ((currentPath.index() == middlePath.index() && secondaryOwner != null)
        || currentPath.isCheckpoint()) return 0;
    if (secondaryOwner == null) return primaryGoal.index();
    return Math.abs(
        currentPath.index() - getNextCheckpoint(currentPath.index() > middlePath.index()).index());
  }

  //

  @Override
  public boolean canComplete(Competitor team) {
    if (team instanceof Team) return team == primaryOwner || team == secondaryOwner;
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

  @Override // Called in ControllableGoal#contbestCycle
  public void dominationCycle(@Nullable Competitor dominatingTeam, int lead, Duration duration) {
    if (!isNeutral() && definition.isPermanent()) return;
    currentOwner = dominatingTeam; // The team that completes the CaptureCondition
  }

  // Ticktickticktick
  public void tick() {

    if (isCompleted()) return; // Freeze if completed

    // Move if conditions are present
    boolean moved = tickMove();

    // Display the pretty circle around the payload
    tickDisplay();

    // Check if any team has gotten further than their last record
    // Returns true if an updated completion is further than the last updated
    // completion for the opposite side
    if (updateCompletion()) leadingTeam = currentOwner;

    // If the payload has moved, trigger an update after calculating the leading team
    if (moved) match.callEvent(new GoalStatusChangeEvent(match, this));
  }

  private boolean tickMove() {

    // Check if the payload has reached a finish
    if (currentPath == primaryGoal || currentPath == secondaryGoal) {
      completed = true; // Freeze the payload on completion
      match.callEvent(new GoalCompleteEvent(match, this, currentOwner, true));

      final ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
      if (smm != null) { // Increment points if the score module is present(default points is 0)
        if (currentOwner != null) smm.incrementScore(currentOwner, definition.getPoints());
      }

      return false; // Dont execute a moving cycle if the payload has been completed
    }

    float speed = getControllingTeamSpeed(); // Fetches the speed for the current owner

    if (speed == 0) return false; // Mainly for if neutral speed is 0

    move(speed / 10.0); // Move the entity
    return true;
  }

  private void move(double distance) {
    if (!currentPath.hasNext() || !currentPath.hasPrevious()) { // Path is over
      payloadEntity.teleport(currentPath.getLocation()); // Teleport to final location
      return;
    }

    // Dont try to do neutral moving if the payload has reached the middle
    if (currentPath.index() == middlePath.index() && (isNeutral() || !canComplete(currentOwner))) {
      payloadEntity.teleport(middlePath.getLocation());
      return;
    }

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
                    ? primaryOwner.getColor()
                    // If any tail checkpoints exist, then the secondary team should NOT be null
                    : secondaryOwner.getColor()));

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
   *     false} if the next checkpoint in the tail direction should be returned}.
   * @return the next checkpoint, or the current checkpoint of there is no more checkpoints in the
   *     given direction
   */
  private PayloadCheckpoint getNextCheckpoint(boolean head) {
    PayloadCheckpoint checkpoint;
    return (checkpoint = checkpointMap.get(lastReachedCheckpointKey - (head ? -1 : 1))) == null
        ? getLastReachedCheckpoint()
        : checkpoint;
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

  /** {@code null} if no checkpoint with the given id exists for this payload */
  @Nullable
  public PayloadCheckpoint getCheckpoint(String id) {
    return checkpointMap.values().stream()
        .filter(
            c -> {
              if (c.getId() != null) return c.getId().equals(id);
              return false;
            })
        .findFirst()
        .orElse(null);
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
    if (canDominate(currentOwner) && currentOwner instanceof Team) {
      Float speed = definition.getSpeed((Team) currentOwner);
      return speed == null ? canComplete(currentOwner) ? 1 : definition.getNeutralSpeed() : speed;
    }

    // If any non-pushing team controls the payload, neutral speed should be used
    return definition.getNeutralSpeed();
  }

  private ChatColor getControllingTeamColor() {
    return isNeutral() ? COLOR_NEUTRAL_TEAM : currentOwner.getColor();
  }

  private boolean isUnderPrimaryOwnerControl() {
    return currentOwner == primaryOwner;
  }

  private boolean isUnderSecondaryOwnerControl() {
    // Ensure that if there is no secondary owner
    // this does not return true if the state is neutral (currentOwner = null)
    return secondaryOwner != null && currentOwner == secondaryOwner;
  }

  private boolean isNeutral() {
    return currentOwner == null;
  }

  protected void makeRail() {
    final Location location = definition.getMiddleLocation().toLocation(getMatch().getWorld());

    if (!isValidStartLocation(location)) return;

    final Rails startingRails = (Rails) location.getBlock().getState().getMaterialData();

    BlockFace direction = startingRails.getDirection();

    middlePath = new Path(location);

    Path previousPath = middlePath;
    Path neighborRail = getNewNeighborPath(previousPath, direction, false);

    boolean flip = false; // Also check the other way
    while (neighborRail != null || flip) {

      if (flip) {
        if (neighborRail == null) {
          break;
        }
        previousPath.setNext(neighborRail);
        neighborRail.setPrevious(previousPath);
      } else {
        previousPath.setPrevious(neighborRail);
        neighborRail.setNext(previousPath);
      }

      previousPath = neighborRail;

      if (previousPath.getLocation().getBlock().getState().getMaterialData() instanceof Rails) {
        direction =
            ((Rails) previousPath.getLocation().getBlock().getState().getMaterialData())
                .getDirection();
      } else {
        direction = null;
      }

      neighborRail = getNewNeighborPath(previousPath, direction, flip);
      if (neighborRail == null && !flip) {
        flip = true; // Try looking the other way for more paths
        previousPath = new Path(previousPath, railSize);
        railSize++;
        while (previousPath
            .hasNext()) { // Iterate over all paths existing at this point to add index numbers
          previousPath.setNext(new Path(previousPath.next(), railSize));
          railSize++;
          previousPath = previousPath.next();
        }
        middlePath = previousPath;
        neighborRail =
            getNewNeighborPath(
                previousPath,
                ((Rails) middlePath.getLocation().getBlock().getState().getMaterialData())
                    .getDirection(),
                flip);
      }
    }

    this.currentPath = middlePath;

    furthestTailPath = middlePath;
    furthestHeadPath = middlePath;

    findGoals();

    makeCheckpoints();
  }

  void findGoals() {
    for (Competitor competitor : match.getCompetitors()) {
      if (competitor instanceof Team) {
        Vector goal;
        goal = definition.getGoal((Team) competitor);
        if (goal != null) {
          Path goalPath = findPath(goal);
          if (goalPath == null) {
            match
                .getLogger()
                .warning("Goal location not found on rail for team: " + competitor.getNameLegacy());
            continue;
          }

          boolean sameSide = false;
          if (goalPath.index() > middlePath.index()) {
            if (primaryOwner != null) sameSide = true;
            primaryOwner = competitor;
            primaryGoal = goalPath;
          } else {
            if (secondaryOwner != null) sameSide = true;
            secondaryOwner = competitor;
            secondaryGoal = goalPath;
          }
          if (sameSide)
            match
                .getLogger()
                .warning("Both goals can not be on the same side of the starting location");
        }
      }
    }
  }

  private void makeCheckpoints() {
    // Checkpoint calculation

    List<PayloadCheckpoint> checkpoints = new ArrayList<>();
    for (PayloadCheckpoint checkpoint : definition.getCheckpoints()) {
      Path checkpointPath = findPath(checkpoint.getLocation());
      if (checkpointPath == null) {
        match
            .getLogger()
            .warning(
                "Checkpoint "
                    + (checkpoint.getId() == null ? "not" : checkpoint.getId() + " not")
                    + " found, location: "
                    + checkpoint.getLocation()
                    + " is not on rail");
        continue;
      }
      checkpoint.setIndex(checkpointPath.index());
      checkpoints.add(checkpoint);
    }

    Comparator<PayloadCheckpoint> sort = Comparator.comparing(PayloadCheckpoint::index);
    checkpoints.sort(sort);
    List<PayloadCheckpoint> headCheckpoints = new ArrayList<>();
    List<PayloadCheckpoint> tailCheckpoints = new ArrayList<>();
    for (PayloadCheckpoint checkpoint : checkpoints) {
      if (checkpoint.index() < middlePath.index()) tailCheckpoints.add(checkpoint);
      else headCheckpoints.add(checkpoint);
    }

    for (int i = 0; i < headCheckpoints.size(); i++) {
      int mapIndex = i + 1;
      PayloadCheckpoint checkpoint = headCheckpoints.get(i);
      if (primaryGoal.index() < checkpoint.index()) {
        match
            .getLogger()
            .warning(
                "Checkpoint "
                    + (checkpoint.getId() == null
                        ? " at " + checkpoint.getLocation()
                        : checkpoint.getId())
                    + "is after the goal and will be ignored");
        continue;
      }
      checkpoint.setMapIndex(mapIndex);
      checkpointMap.put(mapIndex, checkpoint);
      headCheckpointsAmount++;
    }
    // Adding the goal as a checkpoint
    headCheckpointsAmount++;
    checkpointMap.put(
        headCheckpointsAmount,
        new PayloadCheckpoint(
            "primary-goal",
            primaryGoal.getLocation().toVector(),
            false,
            primaryGoal.index(),
            headCheckpointsAmount));

    for (int i = 0; i < tailCheckpoints.size(); i++) {
      int mapIndex = -i - 1;
      PayloadCheckpoint checkpoint = tailCheckpoints.get(i);
      if (secondaryGoal != null && secondaryGoal.index() > checkpoint.index()) {
        match
            .getLogger()
            .warning(
                "Checkpoint "
                    + (checkpoint.getId() == null
                        ? " at " + checkpoint.getLocation()
                        : checkpoint.getId())
                    + "is after the goal and will be ignored");
        continue;
      }
      checkpoint.setMapIndex(mapIndex);
      checkpointMap.put(mapIndex, checkpoint);
      tailCheckpointsAmount++;
    }
    if (tailCheckpointsAmount > 0 && secondaryGoal != null) {
      tailCheckpointsAmount++;
      checkpointMap.put(
          -tailCheckpointsAmount,
          new PayloadCheckpoint(
              "secondary-goal",
              secondaryGoal.getLocation().toVector(),
              false,
              secondaryGoal.index(),
              -tailCheckpointsAmount));
    }

    // Adding the middle as a checkpoint
    PayloadCheckpoint middleCheckpoint =
        new PayloadCheckpoint(
            "middle", middlePath.getLocation().toVector(), false, middlePath.index(), 0);
    checkpointMap.put(0, middleCheckpoint);
  }

  /** Checks if a {@link Material} is any type of rail(normal, detector, activator, powered) */
  public boolean isRails(Material material) {
    // STANDARD_CHECKPOINT_MATERIALS covers all other types of rails
    // The fact that both isRails AND isCheckpoint can be true for a single Path is OK
    return RAIL_MATERIAL_MATCHER.matches(material);
  }

  private boolean isValidStartLocation(Location location) {
    // Payload must start on a rail
    if (!isRails(location.getBlock().getType())) {
      match.getLogger().warning("No rail found in starting position for payload");
      return false;
    }

    final Rails startingRails = (Rails) location.getBlock().getState().getMaterialData();

    if (startingRails.isCurve() || startingRails.isOnSlope()) {
      match.getLogger().warning("Starting rail can not be either curved or in a slope");
      return false;
    }

    if (isCheckpoint(location)) {
      match.getLogger().warning("Starting rail can not be a checkpoint");
      return false;
    }
    return true;
  }

  private boolean isCheckpoint(Location location) {
    return definition.getCheckpoints().stream()
        .anyMatch(c -> c.getLocation().equals(location.toVector()));
  }

  /**
   * Looks along this {@link Payload}s rail for a {@link Path} that has a specific {@link Location}
   *
   * @param lookingFor The point on this rail to look for
   * @return A {@link Path}, or {@code null} if no {@link Path} with the given {@link Location}
   *     exists on this rail
   */
  @Nullable
  private Path findPath(Location lookingFor) {
    Path result = null;
    Path backwards = middlePath;
    while (backwards.hasPrevious()) { // Look behind and onwards
      Location here = backwards.getLocation().toCenterLocation();
      if (here.equals(lookingFor.toCenterLocation())) {
        result = backwards;
        break;
      }
      backwards = backwards.previous();
      if (backwards == null) break;
    }
    if (result == null) { // If not yet found, try looking the other way
      Path forwards = middlePath;
      while (forwards.hasNext()) {
        Location here = forwards.getLocation().toCenterLocation();
        if (here.equals(lookingFor.toCenterLocation())) {
          result = forwards;
          break;
        }
        forwards = forwards.next();
        if (forwards == null) break;
      }
    }
    return result;
  }

  @Nullable
  private Path findPath(Vector lookingFor) {
    return findPath(lookingFor.toLocation(match.getWorld()));
  }

  private Path getNewNeighborPath(Path path, BlockFace direction, boolean flipped) {

    final List<Double> differingX = new ArrayList<>();
    final List<Double> differingY = new ArrayList<>();
    final List<Double> differingZ = new ArrayList<>();

    differingY.add(0.0);
    differingY.add(1.0);
    differingY.add(-1.0);

    Path earlierPath = flipped ? path.previous() : path.next();
    Location previousLocation = null;
    if (earlierPath != null) {
      previousLocation = earlierPath.getLocation();
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
    for (double x : differingX) { // Checking all relevant adjacent blocks
      for (double y : differingY) {
        for (double z : differingZ) {
          newLocation.add(x, y, z);

          boolean isCheckpoint = isCheckpoint(newLocation);

          if (isRails(newLocation.getBlock().getType())) {
            Path currentPath = path;

            boolean alreadyExists = false;
            while (currentPath.hasNext()) {
              if (currentPath.getLocation().equals(newLocation)) {
                alreadyExists = true;
                break;
              }
              currentPath = currentPath.next();
            }

            if (flipped) {
              currentPath = path; // reset
              while (currentPath.hasPrevious()) {
                if (currentPath.getLocation().equals(newLocation)) {
                  alreadyExists = true;
                  break;
                }
                currentPath = currentPath.previous();
              }
            }

            if (!alreadyExists) {
              return new Path(
                  flipped ? railSize++ : 0,
                  newLocation,
                  flipped ? path : null,
                  flipped ? null : path,
                  isCheckpoint);
            }
          }
          // If its not a valid rail block we can ignore it and try the next location
          newLocation.subtract(x, y, z);
        }
      }
    }
    return null; // If there is no more positions to try then there is no new rails(We've hit an
    // end)
  }

  /** Checks if the given {@link Minecart} is the payload minecart */
  public boolean isPayload(Minecart minecart) {
    return minecart == payloadEntity;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Payload) return ((Payload) obj).isPayload(payloadEntity);
    return false;
  }
}
