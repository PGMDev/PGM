package tc.oc.pgm.flag;

import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.BlockVector;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.material.Materials;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.query.IQuery;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.BaseState;
import tc.oc.pgm.flag.state.Captured;
import tc.oc.pgm.flag.state.Completed;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.flag.state.State;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.points.AngleProvider;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.points.StaticAngleProvider;
import tc.oc.pgm.regions.PointRegion;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.server.BukkitUtils;
import tc.oc.util.components.ComponentUtils;

public class Flag extends TouchableGoal<FlagDefinition> implements Listener {

  public static final String RESPAWNING_SYMBOL = "\u2690"; // ⚐
  public static final String RETURNED_SYMBOL = "\u2691"; // ⚑
  public static final String DROPPED_SYMBOL = "\u2691"; // ⚑
  public static final String CARRIED_SYMBOL = "\u2794"; // ➔

  public static final Sound PICKUP_SOUND_OWN = new Sound("mob.wither.idle", 0.7f, 1.2f);
  public static final Sound DROP_SOUND_OWN = new Sound("mob.wither.hurt", 0.7f, 1);
  public static final Sound RETURN_SOUND_OWN = new Sound("mob.zombie.unfect", 1.1f, 1.2f);

  public static final Sound PICKUP_SOUND = new Sound("fireworks.largeBlast_far", 1f, 0.7f);
  public static final Sound DROP_SOUND = new Sound("fireworks.twinkle_far", 1f, 1f);
  public static final Sound RETURN_SOUND = new Sound("fireworks.twinkle_far", 1f, 1f);

  private final ImmutableSet<Net> nets;
  private final Location bannerLocation;
  private final BannerMeta bannerMeta;
  private final ItemStack bannerItem;
  private final AngleProvider bannerYawProvider;
  private final @Nullable Team owner;
  private final Set<Team> capturers;
  private final Set<Team> controllers;
  private final Set<Team> completers;
  private BaseState state;
  private boolean transitioning;

  protected Flag(Match match, FlagDefinition definition, ImmutableSet<Net> nets)
      throws ModuleLoadException {
    super(definition, match);
    this.nets = nets;

    TeamMatchModule tmm = match.getMatchModule(TeamMatchModule.class);

    if (definition.getOwner() != null) {
      this.owner = tmm.getTeam(definition.getOwner());
    } else {
      this.owner = null;
    }

    ImmutableSet.Builder<Team> capturersBuilder = ImmutableSet.builder();
    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        IQuery query = team.getQuery();
        if (getDefinition().canPickup(query) && canCapture(query)) {
          capturersBuilder.add(team);
        }
      }
    }
    this.capturers = capturersBuilder.build();

    ImmutableSet.Builder<Team> controllersBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<Team> completersBuilder = ImmutableSet.builder();
    for (Net net : nets) {
      if (net.getReturnPost() != null && net.getReturnPost().getOwner() != null) {
        Team controller = tmm.getTeam(net.getReturnPost().getOwner());
        controllersBuilder.add(controller);

        if (net.getReturnPost().isPermanent()) {
          completersBuilder.add(controller);
        }
      }
    }
    this.controllers = controllersBuilder.build();
    this.completers = completersBuilder.build();

    Banner banner = null;
    pointLoop:
    for (PointProvider returnPoint : definition.getDefaultPost().getReturnPoints()) {
      Region region = returnPoint.getRegion();
      if (region instanceof PointRegion) {
        // Do not require PointRegions to be at the exact center of the block.
        // It might make sense to just override PointRegion.getBlockVectors() to
        // always do this, but it does technically violate the contract of that method.
        banner =
            toBanner(((PointRegion) region).getPosition().toLocation(match.getWorld()).getBlock());
        if (banner != null) break pointLoop;
      } else {
        for (BlockVector pos : returnPoint.getRegion().getBlockVectors()) {
          banner = toBanner(pos.toLocation(match.getWorld()).getBlock());
          if (banner != null) break pointLoop;
        }
      }
    }

    if (banner == null) {
      throw new ModuleLoadException(
          "Flag '" + getName() + "' must have a banner at its default post");
    }

    this.bannerLocation = Materials.getLocationWithYaw(banner);
    this.bannerMeta = Materials.getItemMeta(banner);
    this.bannerYawProvider = new StaticAngleProvider(this.bannerLocation.getYaw());
    this.bannerItem = new ItemStack(Material.BANNER);
    this.bannerItem.setItemMeta(this.getBannerMeta());
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
    if (color == null) color = this.bannerMeta.getBaseColor();
    return color;
  }

  public net.md_5.bungee.api.ChatColor getChatColor() {
    return ComponentUtils.convert(BukkitUtils.dyeColorToChatColor(this.getDyeColor()));
  }

  public String getColoredName() {
    return this.getChatColor() + this.getName();
  }

  public Component getComponentName() {
    return new PersonalizedText(getName()).color(getChatColor());
  }

  public ImmutableSet<Net> getNets() {
    return nets;
  }

  public BannerMeta getBannerMeta() {
    return bannerMeta;
  }

  public ItemStack getBannerItem() {
    return bannerItem;
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

  public boolean canDropAt(Location location) {
    Block block = location.getBlock();
    Block below = block.getRelative(BlockFace.DOWN);
    if (!canDropOn(below.getState())) return false;
    if (block.getRelative(BlockFace.UP).getType() != Material.AIR) return false;

    switch (block.getType()) {
      case AIR:
      case LONG_GRASS:
        return true;
      default:
        return false;
    }
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
      return new PersonalizedTranslatable("match.flag.pickup.you", getComponentName());
    } else {
      return new PersonalizedTranslatable(
          "match.flag.pickup", getComponentName(), toucher.getStyledName(NameStyle.COLOR));
    }
  }

  // Proximity

  @Override
  public Iterable<Location> getProximityLocations(ParticipantState player) {
    return state.getProximityLocations(player);
  }

  @Override
  public boolean isProximityRelevant(Competitor team) {
    if (hasTouched(team)) {
      return canCapture(team.getQuery());
    } else {
      return canPickup(team.getQuery());
    }
  }

  // Misc

  public void load() {
    this.state = new Returned(this, this.getDefinition().getDefaultPost(), this.bannerLocation);
    this.state.enterState();
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

  public boolean canPickup(IQuery query, Post post) {
    return getDefinition().getPickupFilter().query(query).isAllowed()
        && post.getPickupFilter().query(query).isAllowed();
  }

  public boolean canPickup(IQuery query) {
    return canPickup(query, state.getPost());
  }

  public boolean canPickup(MatchPlayer player, Post post) {
    return canPickup(player.getQuery(), post);
  }

  public boolean canCapture(IQuery query, Net net) {
    return getDefinition().getCaptureFilter().query(query).isAllowed()
        && net.getCaptureFilter().query(query).isAllowed();
  }

  public boolean canCapture(IQuery query) {
    return getDefinition().canCapture(query, getNets());
  }

  public boolean canCapture(MatchPlayer player, Net net) {
    return canCapture(player.getQuery(), net);
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

  public boolean isAtPost(Post post) {
    return this.state.isAtPost(post);
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
  public String renderSidebarStatusText(@Nullable Competitor competitor, Party viewer) {
    return this.state.getStatusText(viewer);
  }

  @Override
  public ChatColor renderSidebarStatusColor(@Nullable Competitor competitor, Party viewer) {
    return this.state.getStatusColor(viewer);
  }

  @Override
  public ChatColor renderSidebarLabelColor(@Nullable Competitor competitor, Party viewer) {
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
    for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext(); ) {
      if (iterator.next().isSimilar(this.getBannerItem())) iterator.remove();
    }
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

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onProjectileHit(EntityDamageEvent event) {
    this.state.onEvent(event);
  }
}
