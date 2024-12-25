package tc.oc.pgm.match;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.Modules;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.api.match.event.MatchResizeEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.match.event.MatchUnloadEvent;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;
import tc.oc.pgm.api.module.ModuleGraph;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.api.party.event.CompetitorAddEvent;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.party.event.PartyAddEvent;
import tc.oc.pgm.api.party.event.PartyRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.events.PlayerLeavePartyEvent;
import tc.oc.pgm.events.PlayerParticipationEvent;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.features.MatchFeatureContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.join.JoinRequest;
import tc.oc.pgm.loot.WorldTickClock;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.FileUtils;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.Events;
import tc.oc.pgm.util.collection.RankedSet;
import tc.oc.pgm.util.concurrent.BukkitExecutorService;

public class MatchImpl implements Match {

  private final String id;
  private final MapContext map;
  private final WeakReference<World> world;
  private final Map<Class<? extends MatchModule>, MatchModule> matchModules;

  private final WorldTickClock clock;
  private final ClassLogger logger;
  private final Random random;
  private final Map<Long, Double> tickRandoms;
  private final AtomicBoolean loaded;
  private final AtomicReference<MatchPhase> state;
  private final AtomicLong start;
  private final AtomicLong end;
  private final AtomicInteger capacity;
  private final EnumMap<MatchScope, ScheduledExecutorService> executors;
  private final EnumMap<MatchScope, Collection<Listener>> listeners;
  private final EnumMap<MatchScope, Collection<Tickable>> tickables;
  private final AtomicReference<Tick> tick;
  private final CountdownContext countdown;
  private final Map<UUID, MatchPlayer> players;
  private final Map<MatchPlayer, Party> partyChanges;
  private final Set<Party> parties;
  private final RankedSet<VictoryCondition> victory;
  private final Set<Competitor> competitors;
  private final RankedSet<Competitor> winners;
  private final AtomicReference<Party> queuedParticipants;
  private final ObserverParty observers;
  private final MatchFeatureContext features;
  private Boolean friendlyFireOverride;

  protected MatchImpl(String id, MapContext map, World world) {
    this.id = assertNotNull(id);
    this.map = assertNotNull(map);
    this.world = new WeakReference<>(assertNotNull(world));
    this.matchModules = new ConcurrentHashMap<>();

    this.clock = new WorldTickClock(this);
    this.logger = ClassLogger.get(PGM.get().getLogger(), getClass());
    this.random = new Random();
    this.tickRandoms = new HashMap<>();
    this.loaded = new AtomicBoolean(false);
    this.state = new AtomicReference<>(MatchPhase.IDLE);
    this.start = new AtomicLong(0);
    this.end = new AtomicLong(0);
    this.capacity = new AtomicInteger(
        map.getInfo().getMaxPlayers().stream().mapToInt(i -> i).sum());
    this.executors = new EnumMap<>(MatchScope.class);
    this.listeners = new EnumMap<>(MatchScope.class);
    this.tickables = new EnumMap<>(MatchScope.class);
    for (MatchScope scope : MatchScope.values()) {
      executors.put(scope, new BukkitExecutorService(PGM.get(), false));
      listeners.put(scope, new LinkedHashSet<>());
      tickables.put(scope, new CopyOnWriteArraySet<>());
    }
    this.tick = new AtomicReference<>(null);
    this.countdown = new SingleCountdownContext(this, logger);
    this.players = new ConcurrentHashMap<>();
    this.partyChanges = new WeakHashMap<>();
    this.parties = new LinkedHashSet<>();
    this.victory = new RankedSet<>(Comparator.<VictoryCondition, Boolean>comparing(
            vc -> !vc.isCompleted(this), Boolean::compare)
        .thenComparing(VictoryCondition::getPriority));
    this.competitors = new HashSet<>();
    this.winners = new RankedSet<>((Competitor a, Competitor b) -> {
      for (VictoryCondition condition : getVictoryConditions()) {
        int result = condition.compare(a, b);
        if (result != 0 || condition.isFinal(this)) return result;
      }
      return 0;
    });
    this.queuedParticipants = new AtomicReference<>();
    this.observers = new ObserverParty(this);
    this.features = new MatchFeatureContext();
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public MatchScope getScope() {
    return isRunning() ? MatchScope.RUNNING : isLoaded() ? MatchScope.LOADED : null;
  }

  @Override
  public boolean isLoaded() {
    return loaded.get();
  }

  @Override
  public MatchPhase getPhase() {
    return state.get();
  }

  @Override
  public boolean setPhase(MatchPhase state) {
    final MatchPhase old = getPhase();
    if (old.canTransitionTo(state) && this.state.compareAndSet(old, state)) {

      switch (state) {
        case RUNNING:
          getModules().forEach(MatchModule::enable);
          start.set(System.currentTimeMillis());
          startListeners(MatchScope.RUNNING);
          startTickables(MatchScope.RUNNING);
          callEvent(new MatchStartEvent(this));
          break;
        case FINISHED:
          winners.invalidateRanking();
          getExecutor(MatchScope.RUNNING).shutdownNow();
          getCountdown().cancelAll();
          callEvent(new MatchFinishEvent(this, winners.getRank(0)));
          break;
      }

      callEvent(new MatchPhaseChangeEvent(this, old, state));

      // Must wait until after event has been called to close listeners
      if (state == MatchPhase.FINISHED) {
        removeListeners(MatchScope.RUNNING);
        getModules().forEach(MatchModule::disable);
        end.set(System.currentTimeMillis());
      }

      return true;
    }
    return false;
  }

  @Override
  public World getWorld() {
    return world.get();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public MapInfo getMap() {
    return map.getInfo();
  }

  @Override
  public ScheduledExecutorService getExecutor(MatchScope scope) {
    return executors.get(scope);
  }

  @Override
  public void callEvent(Event event) {
    PGM.get().getServer().getPluginManager().callEvent(event);
  }

  @Override
  public WorldTickClock getClock() {
    return this.clock;
  }

  @Override
  public Tick getTick() {
    final Tick newTick = this.clock.getTick();
    final Tick oldTick = this.tick.get();
    if (oldTick == null || oldTick != newTick) {
      this.tick.set(this.clock.getTick());
      tickRandoms.clear();
    }
    return this.tick.get();
  }

  @Override
  public double getRandomFromTick(long seed) {
    getTick(); // Clears tickRandoms if a new tick has occurred
    Double number = tickRandoms.get(seed);
    if (number == null) {
      number = random.nextDouble();
      tickRandoms.put(seed, number);
    }
    return number;
  }

  @Override
  public Random getRandom() {
    return random;
  }

  @Override
  public Collection<MatchModule> getModules() {
    return Collections.unmodifiableCollection(matchModules.values());
  }

  @Override
  public <M extends MatchModule> M getModule(Class<? extends M> key) {
    return (M) matchModules.get(key);
  }

  @Override
  public void addListener(Listener listener, MatchScope scope) {
    if (listeners.get(scope).add(listener) && getScope() == scope) {
      startListener(listener);
    }
  }

  private void removeListeners(MatchScope scope) {
    for (Listener listener : listeners.getOrDefault(scope, Collections.emptyList())) {
      HandlerList.unregisterAll(listener);
    }
  }

  private @Nullable MatchScope getListenerScope(Listener thing) {
    return getListenerScope(thing, null);
  }

  private MatchScope getListenerScope(Listener thing, MatchScope def) {
    ListenerScope listenerScope = thing.getClass().getAnnotation(ListenerScope.class);
    return listenerScope == null ? def : listenerScope.value();
  }

  private void startListeners(MatchScope scope) {
    for (Listener listener : listeners.get(scope)) {
      startListener(listener);
    }
  }

  @Override
  public @NotNull Audience audience() {
    final Collection<Audience> audiences = new ArrayList<>(getPlayers());
    audiences.add(Audience.console());
    return Audience.get(audiences);
  }

  private class EventExecutor implements org.bukkit.plugin.EventExecutor {
    private final RegisteredListener listener;

    private EventExecutor(RegisteredListener listener) {
      this.listener = assertNotNull(listener);
    }

    @Override
    public void execute(Listener other, Event event) throws EventException {
      if (((MatchEvent) event).getMatch() == MatchImpl.this) {
        listener.callEvent(event);
      }
    }
  }

  private void startListener(Listener listener) {
    for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : PGM.get()
        .getPluginLoader()
        .createRegisteredListeners(listener, PGM.get())
        .entrySet()) {
      Class<? extends Event> eventClass = entry.getKey();
      HandlerList handlerList = Events.getEventListeners(eventClass);

      // TODO: expand support to other events, such as player, world, entity -- reduce boilerplate
      if (MatchEvent.class.isAssignableFrom(eventClass)) {
        for (final RegisteredListener registeredListener : entry.getValue()) {
          PGM.get()
              .getServer()
              .getPluginManager()
              .registerEvent(
                  eventClass,
                  listener,
                  registeredListener.getPriority(),
                  new EventExecutor(registeredListener),
                  PGM.get());
        }
      } else {
        handlerList.registerAll(entry.getValue());
      }
    }
  }

  @Override
  public void addTickable(Tickable tickable, MatchScope scope) {
    tickables.get(scope).add(tickable);
  }

  private void removeTickable(Tickable tickable) {
    for (MatchScope scope : MatchScope.values()) {
      tickables.get(scope).remove(tickable);
    }
  }

  @Override
  public CountdownContext getCountdown() {
    return countdown;
  }

  @Override
  public MatchFeatureContext getFeatureContext() {
    return features;
  }

  @Override
  public boolean finish(@Nullable Competitor winner) {
    if (winner != null) this.addVictoryCondition(new CompetitorVictoryCondition(winner));
    return setPhase(MatchPhase.FINISHED);
  }

  @Override
  public int getMaxPlayers() {
    return capacity.get();
  }

  @Override
  public void setMaxPlayers(int players) {
    int previous = capacity.getAndSet(players);
    if (previous != players) {
      callEvent(new MatchResizeEvent(this));
    }
  }

  @Override
  public Collection<MatchPlayer> getPlayers() {
    return Collections.unmodifiableCollection(players.values());
  }

  @Override
  public Collection<MatchPlayer> getObservers() {
    final Party queued = queuedParticipants.get();
    if (queued == null) return observers.getPlayers();

    return ImmutableList.<MatchPlayer>builder()
        .addAll(queued.getPlayers())
        .addAll(observers.getPlayers())
        .build();
  }

  @Override
  public Collection<MatchPlayer> getParticipants() {
    final ImmutableList.Builder<MatchPlayer> builder = ImmutableList.builder();

    for (Competitor competitor : getCompetitors()) {
      builder.addAll(competitor.getPlayers());
    }

    return builder.build();
  }

  @Override
  public MatchPlayer addPlayer(Player bukkit) {
    MatchPlayer player = players.get(bukkit.getUniqueId());
    if (player == null) {
      logger.fine("Adding player " + bukkit);

      // If the bukkit player is dead, force them back into the world
      if (bukkit.isDead()) {
        bukkit.leaveVehicle();
        bukkit.spigot().respawn();
      }

      player = new MatchPlayerImpl(this, bukkit);
      MatchPlayerAddEvent event = new MatchPlayerAddEvent(player, getDefaultParty());
      callEvent(event);

      setParty(player, event.getInitialParty());
    }
    return player;
  }

  @Override
  public void removePlayer(Player bukkit) {
    MatchPlayer player = players.get(bukkit.getUniqueId());
    if (player != null) {
      logger.fine("Removing player " + player);
      setOrClearPlayerParty(player, null, JoinRequest.of(null, JoinRequest.Flag.FORCE));
    }
  }

  @Override
  public boolean setParty(MatchPlayer player, Party party, @Nullable JoinRequest request) {
    if (request == null)
      request = party instanceof Team
          ? JoinRequest.of((Team) party, JoinRequest.Flag.FORCE)
          : JoinRequest.force();
    return setOrClearPlayerParty(player, assertNotNull(party), request);
  }

  /**
   * Attempt to add the given player to the given party, and return true if successful. This also
   * handles most of the logic for joining and leaving the match. Doing these things simultaneously
   * is what allows their events to be combined, and ensures that everything is in a consistent
   * state at any point where an event is fired.
   *
   * <p>- If the player is not in the match, they will be added. - If newParty is not in the match,
   * and it is automatic, it will be added. - If newParty is null, the player will be removed from
   * the match, and so will their old party if it is automatic and empty. - If the player is already
   * in newParty, or if the party change is cancelled by {@link PlayerParticipationStartEvent} or
   * {@link PlayerParticipationStopEvent}, none of the above changes will happen, and the method
   * will return false.
   *
   * <p>Order of operations/events:
   *
   * <p>- Call {@link PlayerParticipationStartEvent} and/or {@link PlayerParticipationStopEvent}
   * (and bail if either are cancelled) -
   */
  private boolean setOrClearPlayerParty(
      MatchPlayer player, @Nullable Party newParty, @NotNull JoinRequest joinRequest) {
    Party oldParty = player.getParty();

    assertTrue(this == player.getMatch(), "Player belongs to a different match");
    assertTrue(
        oldParty == null || players.containsValue(player),
        "Joining player is already in the match");
    assertTrue(
        newParty == null || newParty.isAutomatic() || parties.contains(newParty),
        "Party is not in this match and cannot be automatically added");

    if (oldParty == newParty) return false;

    logger.fine("Moving player from " + oldParty + " to " + newParty);

    try {
      // This method is fairly complex and generates a lot of events, so it's worthwhile
      // to detect nested calls for the same player, which we definitely do not want.
      Party nested = partyChanges.put(player, newParty);
      if (nested != null) {
        throw new IllegalStateException("Nested party change: "
            + player
            + " tried to join "
            + newParty
            + " in the middle of joining "
            + nested);
      }

      if (oldParty instanceof Competitor) {
        PlayerParticipationEvent request =
            new PlayerParticipationStopEvent(player, (Competitor) oldParty, joinRequest, newParty);
        callEvent(request);
        // Can't cancel this if the player is leaving the match
        if (request.isCancelled() && newParty != null) {
          if (!Objects.equals(Component.empty(), request.getCancelReason()))
            player.sendWarning(request.getCancelReason());
          return false;
        }
      }

      if (newParty instanceof Competitor) {
        PlayerParticipationEvent request =
            new PlayerParticipationStartEvent(player, (Competitor) newParty, joinRequest);
        callEvent(request);
        // Can't cancel this if the player is joining the match
        if (request.isCancelled() && oldParty != null) {
          if (!Objects.equals(Component.empty(), request.getCancelReason()))
            player.sendWarning(request.getCancelReason());
          return false;
        }
      }

      // Adding the party will fire an event, so do it before any other state changes
      if (newParty != null && newParty.isAutomatic() && !parties.contains(newParty)) {
        addParty(newParty);
      }

      if (oldParty == null) {
        // Player is joining the match
        this.players.put(player.getId(), player);
        addTickable(player, MatchScope.LOADED);
      } else {
        // Player is leaving a party
        if (newParty == null) {
          // If they are not joining a new party, they are also leaving the match
          callEvent(new PlayerLeaveMatchEvent(player, oldParty));
        } else {
          callEvent(new PlayerLeavePartyEvent(player, oldParty));
        }

        // Update the old party's state
        oldParty.removePlayer(player.getId());
      }

      // Update the player's state
      player.internalSetParty(newParty);

      if (newParty == null) {
        // Player is leaving the match, remove them before calling the event.
        // Passing an orphan player to the event is probably safer than leaving them in
        // the match with a null party. Anything that needs to be called before the player
        // is removed should listen for PlayerMatchLeaveEvent.
        removeTickable(player);
        this.players.remove(player.getId());

        callEvent(new PlayerPartyChangeEvent(player, oldParty, null, joinRequest));
      } else {
        // Player is joining a party
        // Update the new party's state
        newParty.addPlayer(player);

        if (oldParty == null) {
          // If they are not leaving an old party, they are also joining the match
          callEvent(new PlayerJoinMatchEvent(player, newParty, joinRequest));
        } else {
          callEvent(new PlayerJoinPartyEvent(player, oldParty, newParty, joinRequest));
        }
      }

      // Removing the party will fire an event, so do it after all other state changes
      if (oldParty != null && oldParty.isAutomatic() && oldParty.getPlayers().isEmpty()) {
        removeParty(oldParty);
      }

      return true;

    } finally {
      partyChanges.remove(player);
    }
  }

  @Override
  public void addVictoryCondition(VictoryCondition condition) {
    if (!isFinished() && victory.add(condition)) {
      logger.fine("Added victory condition " + condition);
      calculateVictory();
    }
  }

  @Override
  public void removeVictoryCondition(VictoryCondition condition) {
    if (victory.remove(condition)) {
      logger.fine("Removed victory condition " + condition);
      calculateVictory();
    }
  }

  @Override
  public Collection<VictoryCondition> getVictoryConditions() {
    victory.invalidateRanking();
    return ImmutableList.copyOf(victory);
  }

  @Override
  public boolean calculateVictory() {
    if (isFinished()) return true;
    if (!isRunning()) return false;

    winners.invalidateRanking();

    logger.fine("Checking for match finish");
    for (VictoryCondition condition : getVictoryConditions()) {
      logger.fine("Checking victory condition " + condition);
      if (condition.isCompleted(this)) {
        logger.fine("Condition " + condition + " is satisfied, ending match");
        finish(null);
        return true;
      }
    }
    return false;
  }

  @Override
  public Party getDefaultParty() {
    return observers;
  }

  @Override
  public Collection<Party> getParties() {
    return ImmutableList.copyOf(parties);
  }

  @Override
  public Collection<Competitor> getCompetitors() {
    return ImmutableList.copyOf(competitors);
  }

  @Override
  public Collection<Competitor> getSortedCompetitors() {
    return ImmutableList.copyOf(winners);
  }

  @Override
  public int getCompetitorIndex(Competitor competitor) {
    return winners.getPosition(competitor);
  }

  @Override
  public void invalidateRanking() {
    winners.invalidateRanking();
  }

  @Override
  public Collection<Competitor> getWinners() {
    winners.invalidateRanking();
    return ImmutableList.copyOf(winners.getRank(0));
  }

  @Override
  public void addParty(Party party) {
    logger.fine("Adding party " + party);
    assertNotNull(party);
    assertTrue(party.getPlayers().isEmpty(), "Party already contains players");
    assertTrue(parties.add(party), "Party is already in this match");

    if (party instanceof Competitor) {
      competitors.add((Competitor) party);
      winners.add((Competitor) party);
    } else if (party instanceof QueuedParty) {
      queuedParticipants.set(party);
    }

    callEvent(
        party instanceof Competitor
            ? new CompetitorAddEvent((Competitor) party)
            : new PartyAddEvent(party));
  }

  @Override
  public void removeParty(Party party) {
    logger.fine("Removing party " + party);

    assertNotNull(party);
    assertTrue(parties.contains(party), "Party is not in this match");
    assertTrue(party.getPlayers().isEmpty(), "Party still has players in it");

    callEvent(
        party instanceof Competitor
            ? new CompetitorRemoveEvent((Competitor) party)
            : new PartyRemoveEvent(party));

    if (party instanceof Competitor) {
      competitors.remove(party);
      winners.remove(party);
    }
    if (party instanceof QueuedParty) queuedParticipants.set(null);
    parties.remove(party);
  }

  @Override
  public Duration getDuration() {
    long start = this.start.get();
    if (start <= 0) {
      return Duration.ZERO;
    }

    long end = this.end.get();
    if (end <= 0) {
      end = System.currentTimeMillis();
    }

    return Duration.ofMillis(end - start);
  }

  @Override
  public boolean getFriendlyFire() {
    return friendlyFireOverride != null ? friendlyFireOverride : map.getInfo().getFriendlyFire();
  }

  @Override
  public void setFriendlyFire(Boolean allow) {
    this.friendlyFireOverride = allow;
  }

  private class TickableTask implements Runnable {
    private final MatchScope scope;

    private TickableTask(MatchScope scope) {
      this.scope = assertNotNull(scope);
    }

    @Override
    public void run() {
      final Tick tick = getTick();
      for (Tickable tickable : MatchImpl.this.tickables.get(scope)) {
        try {
          tickable.tick(MatchImpl.this, tick);
        } catch (Throwable t) {
          logger.log(Level.SEVERE, "Could not tick " + tickable + " in match #" + id, t);
        }
      }
    }
  }

  private void startTickables(MatchScope scope) {
    getExecutor(scope)
        .scheduleAtFixedRate(new TickableTask(scope), 0, TimeUtils.TICK, TimeUnit.MILLISECONDS);
  }

  @Nullable
  public MatchPlayer getPlayer(@Nullable Player player) {
    return player == null ? null : players.get(player.getUniqueId());
  }

  private ImmutableMap<Class<? extends MatchModule>, MatchModuleFactory<?>> buildModuleMap() {
    ImmutableMap.Builder<Class<? extends MatchModule>, MatchModuleFactory<?>> builder =
        ImmutableMap.builder();
    builder.putAll(Modules.MATCH);
    map.getModules()
        .forEach(module -> builder.put(Modules.MAP_TO_MATCH.get(module.getClass()), module));
    return builder.build();
  }

  private class ModuleLoader
      extends ModuleGraph<MatchModule, MatchModuleFactory<? extends MatchModule>> {

    private ModuleLoader() throws ModuleLoadException {
      super(buildModuleMap(), Modules.MATCH_DEPENDENCY_ONLY);
      loadAll();
    }

    @Override
    protected MatchModuleFactory<? extends MatchModule> getFactory(
        Class<? extends MatchModule> key, @Nullable Class<? extends MatchModule> requiredBy)
        throws ModuleLoadException {
      if (key == null) return null;
      try {
        return super.getFactory(key, requiredBy);
      } catch (ModuleLoadException e) {
        return null;
      }
    }

    @Override
    protected void unloadAll() {
      super.unloadAll();
      matchModules.clear();
    }

    @Override
    protected MatchModule createModule(MatchModuleFactory<?> factory) throws ModuleLoadException {
      final MatchModule module = factory.createMatchModule(MatchImpl.this);
      if (module == null) return null;

      module.load();

      matchModules.put(module.getClass(), module);

      if (module instanceof Listener && getListenerScope((Listener) module) == null) {
        logger.warning(module.getClass().getSimpleName()
            + " implements Listener but is not annotated with @ListenerScope");
      }

      if (module instanceof Listener) {
        addListener((Listener) module, getListenerScope((Listener) module, MatchScope.RUNNING));
      }

      if (module instanceof Tickable) {
        addTickable((Tickable) module, MatchScope.LOADED);
      }

      return module;
    }
  }

  @Override
  public void load() throws ModuleLoadException {
    try {
      new ModuleLoader(); // Will load all map and match modules and throw any errors

      for (Feature feature : getFeatureContext().getAll()) {
        if (feature instanceof Listener) {
          addListener((Listener) feature, getListenerScope((Listener) feature, MatchScope.RUNNING));
        }

        if (feature instanceof Tickable) {
          addTickable((Tickable) feature, MatchScope.LOADED);
        }
      }

      startListeners(MatchScope.LOADED);
      addParty(observers);
      loaded.set(true);
      callEvent(new MatchLoadEvent(this));
      // Start ticking after match load event ocurred
      startTickables(MatchScope.LOADED);
    } catch (Throwable e) {
      e.printStackTrace();
      unload();
      destroy();
      throw e;
    }
  }

  @Override
  public void unload() {
    while (!players.isEmpty()) {
      removePlayer(Bukkit.getPlayer(players.keySet().iterator().next()));
    }

    while (!parties.isEmpty()) {
      removeParty(parties.iterator().next());
    }

    if (loaded.getAndSet(false)) {
      callEvent(new MatchUnloadEvent(this));
    }

    getExecutor(MatchScope.RUNNING).shutdownNow();
    getExecutor(MatchScope.LOADED).shutdownNow();
    getCountdown().cancelAll();
    removeListeners(MatchScope.LOADED);

    for (MatchModule matchModule : getModules()) {
      try {
        matchModule.unload();
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Exception unloading " + matchModule, e);
      }
    }

    matchModules.clear();
    listeners.clear();
    tickables.clear();
    players.clear();
    partyChanges.clear();
    parties.clear();
    victory.clear();
    competitors.clear();
    winners.clear();
  }

  @Override
  public void destroy() {
    if (isLoaded()) {
      logger.log(
          Level.SEVERE,
          "Match " + getId() + " is being destroyed without having previously been unloaded");
      unload();
    }

    World world = getWorld();
    this.world.clear();
    if (world == null) return;

    final String worldName = world.getName();
    NMS_HACKS.cleanupWorld(world);
    if (PGM.get().getServer().unloadWorld(worldName, false)) {
      logger.fine("Successfully unloaded " + worldName);
    } else {
      logger.log(
          Level.SEVERE, "Unable to unload world " + worldName + " (this can cause memory leaks!)");
    }

    final File oldMatchFolder = new File(PGM.get().getServer().getWorldContainer(), worldName);
    if (oldMatchFolder.exists()) {
      FileUtils.delete(oldMatchFolder);
    }
  }

  @Override
  @Nullable
  public Filterable<? super MatchQuery> getFilterableParent() {
    return null;
  }

  @Override
  public Collection<? extends Filterable<? extends MatchQuery>> getFilterableChildren() {
    return this.getParties();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R extends Filterable<?>> Collection<? extends R> getFilterableDescendants(Class<R> type) {
    Collection<R> result = new ArrayList<>();
    if (type.isAssignableFrom(Match.class)) {
      result.add((R) this);
    }
    if (type.isAssignableFrom(Party.class)) {
      result.addAll((Collection<? extends R>) getParties());
    }
    if (type.isAssignableFrom(MatchPlayer.class)) {
      result.addAll((Collection<? extends R>) getPlayers());
    }
    return result;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Match && getId().equals(((Match) obj).getId());
  }

  @Override
  public String toString() {
    final World world = this.world.get();
    return "Match{id="
        + this.id
        + ", map="
        + this.map.getInfo().getId()
        + ", world="
        + (world == null ? "<null>" : world.getName())
        + ", phase="
        + this.getPhase()
        + "}";
  }
}
