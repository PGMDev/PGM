package tc.oc.pgm.flag;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.material.ColorUtils.COLOR_UTILS;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.flag.state.BaseState;
import tc.oc.pgm.flag.state.Captured;
import tc.oc.pgm.flag.state.Completed;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.flag.state.Spawned;
import tc.oc.pgm.flag.state.State;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.points.AngleProvider;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.StaticAngleProvider;
import tc.oc.pgm.regions.PointRegion;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class Flag extends TouchableGoal<FlagDefinition> implements Listener {

  public static final Component RESPAWNING_SYMBOL = text("\u2690"); // ⚐
  public static final Component RETURNED_SYMBOL = text("\u2691"); // ⚑
  public static final Component DROPPED_SYMBOL = text("\u2691"); // ⚑
  public static final Component CARRIED_SYMBOL = text("\u2794"); // ➔

  private final ImmutableSet<NetDefinition> nets;
  private final Location bannerLocation;
  private final ColorUtils.BannerData bannerData;
  private final ItemStack bannerItem;
  private final ItemStack legacyBannerItem;
  private final AngleProvider bannerYawProvider;
  private final @Nullable Team owner;
  private Set<Team> capturers;
  private Set<Team> controllers;
  private Set<Team> completers;
  private BaseState state;
  private boolean transitioning;

  protected Flag(Match match, FlagDefinition definition, ImmutableSet<NetDefinition> nets)
      throws ModuleLoadException {
    super(definition, match);
    this.nets = nets;

    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);

    if (definition.getOwner() != null && tmm != null) {
      this.owner = tmm.getTeam(definition.getOwner());
    } else {
      this.owner = null;
    }

    Banner banner = null;
    for (PointProvider point : definition.getDefaultPost().getFallback().getReturnPoints()) {
      if (point.getRegion() instanceof PointRegion r) {
        // Do not require PointRegions to be at the exact center of the block.
        // It might make sense to just override PointRegion.getBlockVectors() to
        // always do this, but it does technically violate the contract of that method.
        banner = toBanner(r.getPosition().toLocation(match.getWorld()).getBlock());
      } else {
        banner = StreamUtils.of(point.getRegion().getBlocks(match.getWorld()))
            .map(Flag::toBanner)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
      }
      if (banner != null) break;
    }

    if (banner == null) {
      throw new ModuleLoadException(
          "Flag '" + getName() + "' must have a banner at its default post");
    }

    this.bannerData = COLOR_UTILS.createBanner(banner);
    this.bannerData.setName(getComponentName());
    this.bannerItem = this.getBannerData().createItem();

    this.bannerLocation = getLocationWithYaw(banner, bannerData.getFacing());
    this.bannerYawProvider = new StaticAngleProvider(this.bannerLocation.getYaw());

    this.legacyBannerItem = new ItemBuilder()
        .material(Materials.WOOL)
        .color(getDyeColor())
        .name(getColoredName())
        .build();
  }

  private static Banner toBanner(Block block) {
    if (block == null) return null;
    BlockState state = block.getState();
    return state instanceof Banner ? (Banner) state : null;
  }

  @Override
  public String toString() {
    return "Flag{name=" + this.getName() + " state=" + this.state + "}";
  }

  public DyeColor getDyeColor() {
    DyeColor color = this.getDefinition().getColor();
    if (color == null) color = bannerData.getBaseColor();
    return color;
  }

  public ChatColor getBukkitColor() {
    return BukkitUtils.dyeColorToChatColor(this.getDyeColor());
  }

  public TextColor getTextColor() {
    return TextFormatter.convert(this.getBukkitColor());
  }

  @Override
  public Component getComponentName() {
    return super.getComponentName().color(TextFormatter.convert(getBukkitColor()));
  }

  public String getColoredName() {
    return TextTranslations.translateLegacy(getComponentName());
  }

  public ImmutableSet<NetDefinition> getNets() {
    return nets;
  }

  public ColorUtils.BannerData getBannerData() {
    return bannerData;
  }

  public ItemStack getBannerItem() {
    return bannerItem;
  }

  public ItemStack getLegacyBannerItem() {
    return legacyBannerItem;
  }

  public BaseState getState() {
    return state;
  }

  public Optional<Location> getLocation() {
    return this.state instanceof Spawned
        ? Optional.of(((Spawned) state).getLocation())
        : Optional.empty();
  }

  /** Owner is defined in XML, and does not change during a match */
  public @Nullable Team getOwner() {
    return owner;
  }

  /** Controller is the owner of the {@link Post} the flag is at, which obviously can change */
  public @Nullable Team getController() {
    return this.state.getController();
  }

  public boolean hasMultipleControllers() {
    return !controllers.isEmpty();
  }

  public boolean canDropOn(BlockState base) {
    return base.getType().isSolid()
        || (getDefinition().canDropOnWater() && Materials.isWater(base.getType()));
  }

  public boolean canDrop(LocationQuery query) {
    return canDropAt(query.getLocation())
        && getDefinition().getDropFilter().query(query).isAllowed();
  }

  public boolean canDropAt(Location location) {
    Block block = location.getBlock();
    Block below = block.getRelative(BlockFace.DOWN);
    if (!canDropOn(below.getState())) return false;
    if (block.getRelative(BlockFace.UP).getType() != Material.AIR) return false;
    return block.getType() == Material.AIR || block.getType() == Materials.SHORT_GRASS;
  }

  public Post getPost(PostDefinition post) {
    return post == null ? null : match.needModule(FlagMatchModule.class).getPost(post);
  }

  public Location getReturnPoint(Post post) {
    return post.getReturnPoint(this, this.bannerYawProvider).clone();
  }

  // Touchable

  @Override
  public boolean canTouch(ParticipantState player) {
    MatchPlayer matchPlayer = player.getPlayer().orElse(null);
    return matchPlayer != null && canPickup(matchPlayer, state.getPost());
  }

  @Override
  public boolean showEnemyTouches() {
    return true;
  }

  @Override
  public Component getTouchMessage(ParticipantState toucher, boolean self) {
    if (self) {
      return translatable("flag.touch.you", getComponentName());
    } else {
      return translatable(
          "flag.touch.player", getComponentName(), toucher.getName(NameStyle.COLOR));
    }
  }

  // Proximity

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    return state.getProximityLocations(player);
  }

  @Override
  public boolean isProximityRelevant(Competitor team) {
    return getProximityMetric(team) != null
        && (hasTouched(team) ? canCapture(team.getQuery()) : canPickup(team.getQuery()));
  }

  // Misc

  public void load(FlagMatchModule fmm) {
    TeamMatchModule tmm = match.getModule(TeamMatchModule.class);
    ImmutableSet.Builder<Team> capturersBuilder = ImmutableSet.builder();
    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        Query query = team.getQuery();
        if (getDefinition().canPickup(query) && canCapture(query)) {
          capturersBuilder.add(team);
        }
      }
    }
    this.capturers = capturersBuilder.build();

    ImmutableSet.Builder<Team> controllersBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<Team> completersBuilder = ImmutableSet.builder();
    for (NetDefinition net : nets) {
      PostDefinition netPost = net.getReturnPost();
      if (netPost != null && netPost.getFallback().getOwner() != null && tmm != null) {
        Team controller = tmm.getTeam(netPost.getFallback().getOwner());
        controllersBuilder.add(controller);

        if (net.getReturnPost().getFallback().isPermanent()) {
          completersBuilder.add(controller);
        }
      }
    }
    this.controllers = controllersBuilder.build();
    this.completers = completersBuilder.build();

    this.state =
        new Returned(this, fmm.getPost(this.getDefinition().getDefaultPost()), this.bannerLocation);
    this.state.enterState();
  }

  static Location getLocationWithYaw(Banner block, BlockFace facing) {
    Location location = block.getLocation();
    location.setYaw(BlockFaces.faceToYaw(facing));
    return location;
  }

  /**
   * Transition to the given state. This happens immediately if not already transitioning. If this
   * is called from within a transition, the state is queued and the transition happens after the
   * current one completes. This allows {@link BaseState#enterState} to immediately transition into
   * another state without nesting the transitions, and keeps the events in the correct order.
   */
  public void transition(BaseState newState) {
    if (this.transitioning) {
      throw new IllegalStateException("Nested flag state transition");
    }

    BaseState oldState = this.state;
    try {
      logger.fine("Transitioning " + getName() + " from " + oldState + " to " + newState);

      this.transitioning = true;
      this.state.leaveState();
      this.state = newState;
      this.state.enterState();
    } finally {
      this.transitioning = false;
    }

    getMatch().callEvent(new FlagStateChangeEvent(this, oldState, this.state));

    // If we are still in the state we just transitioned into, start the countdown, if any.
    // We check this because the FlagStateChangeEvent may have already transitioned into another
    // state.
    if (this.state == newState) {
      this.state.startCountdown();
    }

    // Check again, in case startCountdown transitioned. In that case, the nested
    // transition will have already called these events if necessary.
    if (this.state == newState) {
      getMatch().callEvent(new GoalStatusChangeEvent(getMatch(), this));
      if (isCompleted())
        getMatch().callEvent(new GoalCompleteEvent(getMatch(), this, this.getController(), true));
    }
  }

  public boolean canPickup(Query query, Post post) {
    return getDefinition().getPickupFilter().query(query).isAllowed()
        && post.getPickupFilter().query(query).isAllowed();
  }

  public boolean canPickup(Query query) {
    return canPickup(query, state.getPost());
  }

  public boolean canCapture(Query query, NetDefinition net) {
    return getDefinition().getCaptureFilter().query(query).isAllowed()
        && net.getCaptureFilter().query(query).isAllowed();
  }

  public boolean canCapture(Query query) {
    return getDefinition().canCapture(query, getNets());
  }

  public boolean isCurrent(Class<? extends State> state) {
    return state.isInstance(this.state);
  }

  public boolean isCurrent(State state) {
    return this.state == state;
  }

  public boolean isCarrying(ParticipantState player) {
    MatchPlayer matchPlayer = player.getPlayer().orElse(null);
    return matchPlayer != null && isCarrying(matchPlayer);
  }

  public boolean isCarrying(MatchPlayer player) {
    return this.state.isCarrying(player);
  }

  public boolean isCarrying(Competitor party) {
    return this.state.isCarrying(party);
  }

  public boolean isAtPost(PostDefinition post) {
    return this.state.getPost().getDefinition() == post
        || this.state.getPost().getCurrent() == post;
  }

  public boolean isCompletable() {
    return !completers.isEmpty();
  }

  @Override
  public boolean canComplete(Competitor team) {
    return team instanceof Team && capturers.contains(team);
  }

  @Override
  public boolean isShared() {
    // Flag is shared if it has multiple capturers or no capturers
    return capturers.size() != 1;
  }

  @Override
  public boolean isCompleted() {
    return isCurrent(Completed.class);
  }

  @Override
  public boolean isCompleted(Competitor team) {
    return isCompleted() && getController() == team;
  }

  public boolean isCaptured() {
    return isCompleted() || isCurrent(Captured.class);
  }

  @Override
  public Component renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return this.state.getStatusText(viewer);
  }

  @Override
  public TextColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return this.state.getStatusColor(viewer);
  }

  @Override
  public TextColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
    return this.state.getLabelColor(viewer);
  }

  /**
   * Play one of two status sounds depending on the team of the listener. Owning players hear the
   * first sound, other players hear the second.
   */
  public void playStatusSound(Sound ownerSound, Sound otherSound) {
    for (MatchPlayer listener : getMatch().getPlayers()) {
      if (listener.getParty() != null
          && (listener.getParty() == this.getOwner()
              || listener.getParty() == this.getController())) {
        listener.playSound(ownerSound);
      } else {
        listener.playSound(otherSound);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    event.getDrops().removeIf(itemStack -> itemStack.isSimilar(this.getBannerItem()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onGoalChange(GoalEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagStateChange(FlagStateChangeEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onFlagCapture(FlagCaptureEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event.getFrom().getWorld() == event.getTo().getWorld()) { // yes, this can be false
      this.state.onEvent(event);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockTransform(BlockTransformEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemDrop(PlayerDropItemEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(ParticipantDespawnEvent event) {
    this.state.onEvent(event);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    this.state.onEvent(event);
  }
}
