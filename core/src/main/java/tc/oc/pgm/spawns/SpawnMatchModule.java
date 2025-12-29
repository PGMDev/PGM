package tc.oc.pgm.spawns;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerChangePartyEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.events.PlayerPartyChangeEventBase;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.spawns.states.Joining;
import tc.oc.pgm.spawns.states.Observing;
import tc.oc.pgm.spawns.states.State;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.event.player.PlayerAttackEntityEvent;

@SuppressWarnings("UnstableApiUsage")
@ListenerScope(MatchScope.LOADED)
public class SpawnMatchModule implements MatchModule, Listener, Tickable {

  private static final long PREDICTED_EXTRA_TICKS = 10 * 20;

  private final Match match;
  private final SpawnModule module;
  private final Map<MatchPlayer, State> states = new HashMap<>();
  private final ListMultimap<MatchPlayer, State> transitions = ArrayListMultimap.create();

  private final Map<Competitor, Spawn> unique = new HashMap<>();
  private final Set<Spawn> failed = new HashSet<>();

  // Join timeouts
  private final Cache<UUID, Long> deathTicks =
      CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build();
  private final Cache<UUID, ParticipationData> participationData =
      CacheBuilder.newBuilder().expireAfterWrite(120, TimeUnit.SECONDS).build();

  public SpawnMatchModule(Match match, SpawnModule module) {
    this.match = match;
    this.module = module;
  }

  public Match getMatch() {
    return match;
  }

  public RespawnOptions getRespawnOptions(Query query) {
    return module.respawnOptions.stream()
        .filter(respawn -> respawn.filter.query(query) == Filter.QueryResponse.ALLOW)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No respawn option could be used"));
  }

  public Spawn getDefaultSpawn() {
    return module.defaultSpawn;
  }

  public List<Spawn> getSpawns() {
    return module.spawns;
  }

  public List<Kit> getPlayerKits() {
    return module.playerKits;
  }

  /** Return all {@link Spawn}s that the given player is currently allowed to spawn at */
  public List<Spawn> getSpawns(MatchPlayer player) {
    List<Spawn> result = Lists.newArrayList();
    for (Spawn spawn : this.getSpawns()) {
      if (spawn.allows(player)) {
        result.add(spawn);
      }
    }
    return result;
  }

  /**
   * Return a randomly chosen {@link Spawn} that the given player is currently allowed to spawn at,
   * or null if none are available. If a team is given, assume the player will have switched to that
   * team by the time they spawn.
   */
  public @Nullable Spawn chooseSpawn(MatchPlayer player) {
    Competitor competitor = player.getCompetitor();
    if (player.isObserving()) {
      return getDefaultSpawn();
    } else if (competitor != null && unique.containsKey(competitor)) {
      return unique.get(competitor);
    } else {
      List<Spawn> potential = getSpawns(player);
      potential.removeAll(unique.values());
      if (!potential.isEmpty()) {
        Spawn spawn = potential.get(match.getRandom().nextInt(potential.size()));
        if (spawn.attributes.exclusive) unique.put(competitor, spawn);
        return spawn;
      } else {
        return null;
      }
    }
  }

  public void reportFailedSpawn(Spawn spawn, MatchPlayer player) {
    if (failed.add(spawn)) {
      // Note: PGM no longer keeps Document data after map parsing
      // Element elSpawn = match.getMap().getFeatures().getNode(spawn);
      ModuleLoadException ex = new ModuleLoadException(
          "Failed to generate spawn location for " + player.getBukkit().getName());
      PGM.get().getGameLogger().log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  public long getDeathTick(MatchPlayer player) {
    Long deathTick = deathTicks.getIfPresent(player.getId());
    return deathTick != null ? deathTick : 0;
  }

  public long getJoinPenalty(PlayerPartyChangeEventBase event) {
    if (event.getRequest().has(JoinRequest.Flag.FORCE)) return 0;
    if (event.getNewParty() == null || !event.getNewParty().isParticipating()) return 0;

    ParticipationData data = participationData.getIfPresent(event.getPlayer().getId());
    return data == null ? 0 : data.getJoinTick(event.getNewParty() instanceof Team t ? t : null);
  }

  private void leaveState(MatchPlayer player) {
    final State state = states.get(player);
    if (state != null) {
      state.leaveState();
      states.remove(player);
    }
  }

  private void enterState(MatchPlayer player, State state) {
    states.put(player, state);
    state.enterState();
  }

  private void changeState(MatchPlayer player, @Nullable State state) {
    leaveState(player);
    if (state != null) {
      enterState(player, state);
    }
  }

  private boolean hasQueuedTransitions(MatchPlayer player) {
    return transitions.containsKey(player);
  }

  private void processQueuedTransitions(MatchPlayer player) {
    final List<State> queue = transitions.get(player);
    while (!queue.isEmpty()) {
      changeState(player, queue.removeFirst());
    }
  }

  public void transition(MatchPlayer player, @Nullable State newState) {
    transitions.put(player, newState);
  }

  private void withState(@Nullable MatchPlayer player, Consumer<State> consumer) {
    if (player == null) return;
    var state = states.get(player);
    if (state != null) consumer.accept(state);
  }

  private void withState(@Nullable Entity bukkit, BiConsumer<MatchPlayer, State> consumer) {
    final MatchPlayer player = match.getPlayer(bukkit);
    withState(player, state -> consumer.accept(player, state));
  }

  private void dispatchEvent(@Nullable MatchPlayer player, Consumer<State> consumer) {
    withState(player, state -> {
      consumer.accept(state);
      processQueuedTransitions(player);
    });
  }

  private void dispatchEvent(@Nullable Entity bukkit, BiConsumer<MatchPlayer, State> consumer) {
    withState(bukkit, (player, state) -> {
      consumer.accept(player, state);
      processQueuedTransitions(player);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLeave(final PlayerParticipationStopEvent event) {
    MatchPlayer player = event.getPlayer();
    // Match not started, or you never fully joined, or forced move
    if (!event.getMatch().isRunning() || states.get(player) instanceof Joining) return;
    if (event.getRequest().has(JoinRequest.Flag.FORCE) && event.getNextParty() != null) return;

    ParticipationData data = participationData.getIfPresent(event.getPlayer().getId());
    (data == null ? data = new ParticipationData() : data).trackLeave(match, event.getCompetitor());
    participationData.put(event.getPlayer().getId(), data);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(final PlayerChangePartyEvent event) {
    MatchPlayer player = event.getPlayer();
    if (event.getOldParty() == null) {
      // Join match
      var newState = event.getNewParty().isParticipating()
          ? new Joining(player, getJoinPenalty(event), false)
          : new Observing(player, true, true);
      event.addHandler(() -> enterState(player, newState));
    } else {
      withState(player, state -> {
        state.onEvent(event);

        if (hasQueuedTransitions(player)) {
          // If the party change caused a state transition, leave the old
          // state before the change, and enter the new state afterward.

          // The potential danger here is that the player has no spawn state
          // during the party change, while other events are firing. The
          // danger is minimized by listening at MONITOR priority.
          leaveState(player);
          event.addHandler(() -> processQueuedTransitions(player));
        }
      });
    }
  }

  /** Must run before {@link tc.oc.pgm.tracker.trackers.DeathTracker#onPlayerDeath} */
  @EventHandler(priority = EventPriority.LOW)
  public void onVanillaDeath(final PlayerDeathEvent event) {
    dispatchEvent(event.getEntity(), (player, state) -> state.onEvent(event));
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onDeath(final MatchPlayerDeathEvent event) {
    dispatchEvent(event.getVictim(), state -> state.onEvent(event));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryClick(final InventoryClickEvent event) {
    dispatchEvent(event.getWhoClicked(), (player, state) -> state.onEvent(event));
  }

  // Listen on HIGH so the picker can handle this first
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onObserverInteract(final ObserverInteractEvent event) {
    dispatchEvent(event.getPlayer(), state -> state.onEvent(event));
  }

  @EventHandler
  public void onAttackEntity(final PlayerAttackEntityEvent event) {
    dispatchEvent(event.getPlayer(), (p, state) -> state.onEvent(event));
  }

  @EventHandler
  public void onTransferItem(final PlayerItemTransferEvent event) {
    dispatchEvent(event.getPlayer(), (p, state) -> state.onEvent(event));
  }

  @EventHandler
  public void onPlayerDamage(final EntityDamageEvent event) {
    dispatchEvent(event.getEntity(), (p, state) -> state.onEvent(event));
  }

  @EventHandler
  public void matchBegin(final MatchStartEvent event) {
    // Copy states so they can transition without concurrent modification
    ImmutableMap.copyOf(states).forEach((player, state) -> {
      state.onEvent(event);
      processQueuedTransitions(player);
    });
  }

  @EventHandler
  public void matchEnd(final MatchFinishEvent event) {
    // Copy states so they can transition without concurrent modification
    ImmutableMap.copyOf(states).forEach((player, state) -> {
      // This event can be fired from inside a party change, so some players may have no party
      if (player.getParty() != null) {
        state.onEvent(event);
        processQueuedTransitions(player);
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInitialSpawn(final PlayerSpawnLocationEvent event) {
    // Ensure the player spawns in the match world
    event.setSpawnLocation(match.getWorld().getSpawnLocation());
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
  public void teleportObservers(final EntityDamageEvent event) {
    // When an observer begins to take fall damage, teleport them to their spawn
    // Due to a bug causing infinite TP loop, only tp twice a second at most
    if (event.getEntity() instanceof Player
        && event.getCause() == EntityDamageEvent.DamageCause.VOID
        && (match.getTick().tick % 10) == 0) {
      MatchPlayer player = match.getPlayer(event.getEntity());
      if (player != null && player.isObserving()) {
        Spawn spawn = chooseSpawn(player);
        if (spawn != null) {
          Location location = spawn.getSpawn(player);
          if (location != null) {
            player.getBukkit().teleport(location);
          }
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onCompetitorRemove(CompetitorRemoveEvent event) {
    // If a competitor is no longer valid, free up its provider
    Competitor competitor = event.getCompetitor();
    if (unique.containsKey(competitor)) {
      Spawn spawn = unique.get(competitor);
      // Do not change if persistence is enabled
      if (!spawn.attributes.persistent) {
        unique.remove(competitor);
      }
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    long tick = event.getMatch().getTick().tick;

    if (event.isPredicted()) {
      tick = tick + PREDICTED_EXTRA_TICKS;
    }

    deathTicks.put(event.getPlayer().getId(), tick);
  }

  @Override
  public void tick(Match match, Tick tick) {
    // Copy states so they can transition without concurrent modification
    ImmutableMap.copyOf(states).forEach((player, state) -> {
      state.tick();
      processQueuedTransitions(player);
    });
  }
}
