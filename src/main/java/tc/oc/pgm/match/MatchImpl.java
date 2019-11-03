package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.*;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.party.event.CompetitorAddEvent;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.party.event.PartyAddEvent;
import tc.oc.pgm.api.party.event.PartyRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.*;
import tc.oc.pgm.features.Feature;
import tc.oc.pgm.features.MatchFeatureContext;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.filters.query.Query;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.module.ModuleInfo;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.result.VictoryCondition;
import tc.oc.server.Events;
import tc.oc.server.Scheduler;
import tc.oc.util.collection.RankedSet;
import tc.oc.util.logging.ClassLogger;
import tc.oc.world.NMSHacks;

public class MatchImpl implements Match, Comparable<Match> {

  private final String id;
  private final WeakReference<PGMMap> map;
  private final WeakReference<World> world;

  private final ClassLogger logger;
  private final Random random;
  private final Map<Long, Double> tickRandoms;
  private final AtomicBoolean loaded;
  private final AtomicReference<MatchPhase> state;
  private final AtomicLong start;
  private final AtomicInteger capacity;
  private final EnumMap<MatchScope, Scheduler> schedulers;
  private final EnumMap<MatchScope, Collection<Listener>> listeners;
  private final EnumMap<MatchScope, Collection<Tickable>> tickables;
  private final AtomicReference<Tick> tick;
  private final CountdownContext countdown;
  private final MatchQuery query;
  private final MatchModuleContext context;
  private final Map<UUID, MatchPlayer> players;
  private final Map<MatchPlayer, Party> partyChanges;
  private final Set<Party> parties;
  private final Set<VictoryCondition> victory;
  private final RankedSet<Competitor> competitors;
  private final Observers observers;

  protected MatchImpl(String id, PGMMap map, World world) {
    this.id = checkNotNull(id);
    this.map = new WeakReference<>(checkNotNull(map));
    this.world = new WeakReference<>(checkNotNull(world));

    this.logger = ClassLogger.get(PGM.get().getLogger(), getClass());
    this.random = new Random();
    this.tickRandoms = new HashMap<>();
    this.loaded = new AtomicBoolean(false);
    this.state = new AtomicReference<>(MatchPhase.IDLE);
    this.start = new AtomicLong(0);
    this.capacity = new AtomicInteger(map.getPersistentContext().getMaxPlayers());
    this.schedulers = new EnumMap<>(MatchScope.class);
    this.listeners = new EnumMap<>(MatchScope.class);
    this.tickables = new EnumMap<>(MatchScope.class);
    for (MatchScope scope : MatchScope.values()) {
      schedulers.put(scope, new Scheduler(PGM.get()));
      listeners.put(scope, new LinkedList<>());
      tickables.put(scope, new CopyOnWriteArraySet<>());
    }
    this.tick = new AtomicReference<>(null);
    this.countdown = new SingleCountdownContext(this, logger);
    this.query = new MatchQuery(null, this);
    this.context = new MatchModuleContext(new MatchFeatureContext());
    this.players = new HashMap<>();
    this.partyChanges = new WeakHashMap<>();
    this.parties = new LinkedHashSet<>();
    this.victory = new LinkedHashSet<>();
    this.competitors =
        new RankedSet<>(
            (Competitor a, Competitor b) -> {
              for (VictoryCondition condition : getVictoryConditions()) {
                int result = condition.compare(a, b);
                if (result != 0 || condition.isFinal(this)) return result;
              }
              return 0;
            });
    this.observers = new Observers(this);

    // TODO: Protect against someone else unloading the world, cancel the event
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public MatchScope getScope() {
    return isRunning() ? MatchScope.RUNNING : loaded.get() ? MatchScope.LOADED : null;
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
          getModuleContext().getAll().forEach(MatchModule::enable);
          start.set(System.currentTimeMillis());
          startListeners(MatchScope.RUNNING);
          startTickables(MatchScope.RUNNING);
          callEvent(new MatchStartEvent(this));
          break;
        case FINISHED:
          calculateVictory();
          getScheduler(MatchScope.RUNNING).cancel();
          getCountdown().cancelAll();
          callEvent(new MatchFinishEvent(this, competitors.getRank(0)));
          break;
      }

      callEvent(new MatchPhaseChangeEvent(this, old, state));

      // Must wait until after event has been called to close listeners
      if (state == MatchPhase.FINISHED) {
        removeListeners(MatchScope.RUNNING);
        getModuleContext().getAll().forEach(MatchModule::disable);
      }

      getPlayers().forEach(MatchPlayer::resetGamemode);
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
  public PGMMap getMap() {
    return map.get();
  }

  @Override
  public Scheduler getScheduler(MatchScope scope) {
    return schedulers.get(scope);
  }

  @Override
  public void callEvent(Event event) {
    PGM.get().getServer().getPluginManager().callEvent(event);
  }

  @Override
  public Tick getTick() {
    long now = NMSHacks.getMonotonicTime(getWorld());
    Tick old = tick.get();
    if (old == null || old.tick != now) {
      tick.set(new Tick(now, Instant.now()));
      tickRandoms.clear();
    }
    return tick.get();
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
  public <T extends MatchModule> Optional<T> getModule(Class<T> moduleClass) {
    return Optional.ofNullable(getModuleContext().getMatchModule(moduleClass));
  }

  @Override
  public void addListener(Listener listener, MatchScope scope) {
    if (listeners.get(scope).add(listener) && getScope() == scope) {
      startListener(listener);
    }
  }

  private void removeListeners(MatchScope scope) {
    for (Listener listener : listeners.get(scope)) {
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

  private void startListener(Listener listener) {
    for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry :
        PGM.get().getPluginLoader().createRegisteredListeners(listener, PGM.get()).entrySet()) {
      Class<? extends Event> eventClass = entry.getKey();
      HandlerList handlerList = Events.getEventListeners(eventClass);

      // TODO: expand support to other events, such as player, world, entity -- reduce boilerplate
      // "ifs"
      if (MatchEvent.class.isAssignableFrom(eventClass)) {
        for (final RegisteredListener registeredListener : entry.getValue()) {
          PGM.get()
              .getServer()
              .getPluginManager()
              .registerEvent(
                  eventClass,
                  listener,
                  registeredListener.getPriority(),
                  new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) throws EventException {
                      if (((MatchEvent) event).getMatch() == MatchImpl.this) {
                        registeredListener.callEvent(event);
                      }
                    }
                  },
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
  public MatchModuleContext getModuleContext() {
    return context;
  }

  @Override
  public MatchFeatureContext getFeatureContext() {
    return getModuleContext().matchFeatureContext;
  }

  @Override
  public MapModuleContext getMapContext() {
    return getMap()
        .getContext()
        .orElseThrow(() -> new IllegalStateException("Map context not found (was it unloaded?)"));
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
      callEvent(new MatchResizeEvent(null));
    }
  }

  @Override
  public Collection<MatchPlayer> getPlayers() {
    return ImmutableSet.copyOf(players.values());
  }

  @Override
  public Collection<MatchPlayer> getObservers() {
    return Collections2.filter(getPlayers(), player -> player.isObserving());
  }

  @Override
  public Collection<MatchPlayer> getParticipants() {
    return Collections2.filter(getPlayers(), player -> player.isParticipating());
  }

  @Override
  public MatchPlayer addPlayer(Player bukkit) {
    MatchPlayer player = players.get(bukkit.getUniqueId());
    if (player == null) {
      logger.fine("Adding player " + bukkit);

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
      setOrClearPlayerParty(player, null);
    }
  }

  @Override
  public boolean setParty(MatchPlayer player, Party party) {
    return setOrClearPlayerParty(player, checkNotNull(party));
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
  private boolean setOrClearPlayerParty(MatchPlayer player, @Nullable Party newParty) {
    Party oldParty = player.getParty();

    checkState(this == player.getMatch(), "Player belongs to a different match");
    checkState(
        oldParty == null || players.containsValue(player),
        "Joining player is already in the match");
    checkState(
        newParty == null || newParty.isAutomatic() || parties.contains(newParty),
        "Party is not in this match and cannot be automatically added");

    if (oldParty == newParty) return false;

    logger.fine("Moving player from " + oldParty + " to " + newParty);

    try {
      // This method is fairly complex and generates a lot of events, so it's worthwhile
      // to detect nested calls for the same player, which we definitely do not want.
      Party nested = partyChanges.put(player, newParty);
      if (nested != null) {
        throw new IllegalStateException(
            "Nested party change: "
                + player
                + " tried to join "
                + newParty
                + " in the middle of joining "
                + nested);
      }

      if (oldParty instanceof Competitor) {
        PlayerParticipationEvent request =
            new PlayerParticipationStopEvent(player, (Competitor) oldParty);
        callEvent(request);
        if (request.isCancelled()
            && newParty != null) { // Can't cancel this if the player is leaving the match
          player.sendWarning(request.getCancelReason(), true);
          return false;
        }
      }

      if (newParty instanceof Competitor) {
        PlayerParticipationEvent request =
            new PlayerParticipationStartEvent(player, (Competitor) newParty);
        callEvent(request);
        if (request.isCancelled()
            && oldParty != null) { // Can't cancel this if the player is joining the match
          player.sendWarning(request.getCancelReason(), true);
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
        oldParty.internalRemovePlayer(player);
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

        callEvent(new PlayerPartyChangeEvent(player, oldParty, null));
      } else {
        // Player is joining a party
        // Update the new party's state
        newParty.internalAddPlayer(player);

        if (oldParty == null) {
          // If they are not leaving an old party, they are also joining the match
          callEvent(new PlayerJoinMatchEvent(player, newParty));
        } else {
          callEvent(new PlayerJoinPartyEvent(player, oldParty, newParty));
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
    if (victory.add(condition)) {
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
    return ImmutableSet.copyOf(victory);
  }

  @Override
  public boolean calculateVictory() {
    if (isFinished()) return true;
    if (!isRunning()) return false;

    competitors.invalidateRanking();

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
    return ImmutableSet.copyOf(parties);
  }

  @Override
  public Collection<Competitor> getCompetitors() {
    return ImmutableSet.copyOf(competitors);
  }

  @Override
  public void addParty(Party party) {
    logger.fine("Adding party " + party);
    checkNotNull(party);
    checkState(party.getPlayers().isEmpty(), "Party already contains players");
    checkState(parties.add(party), "Party is already in this match");

    if (party instanceof Competitor) {
      competitors.add((Competitor) party);
    }

    callEvent(
        party instanceof Competitor
            ? new CompetitorAddEvent((Competitor) party)
            : new PartyAddEvent(party));
  }

  @Override
  public void removeParty(Party party) {
    logger.fine("Removing party " + party);

    checkNotNull(party);
    checkState(parties.contains(party), "Party is not in this match");
    checkState(party.getPlayers().isEmpty(), "Party still has players in it");

    callEvent(
        party instanceof Competitor
            ? new CompetitorRemoveEvent((Competitor) party)
            : new PartyRemoveEvent(party));

    if (party instanceof Competitor) competitors.remove(party);
    parties.remove(party);
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public Duration getDuration() {
    if (start.get() <= 0) {
      return Duration.ZERO;
    }
    return new Duration(start.get(), System.currentTimeMillis());
  }

  private void startTickables(MatchScope scope) {
    getScheduler(scope)
        .runTaskTimer(
            1,
            () -> {
              final Tick tick = getTick();
              tickables.get(scope).forEach(item -> item.tick(this, tick));
            });
  }

  @Nullable
  public MatchPlayer getPlayer(@Nullable Player player) {
    return player == null ? null : players.get(player.getUniqueId());
  }

  @Override
  public Iterable<? extends Audience> getAudiences() {
    return Iterables.concat(
        getPlayers(), Collections.singleton(Audience.get(Bukkit.getConsoleSender())));
  }

  class ModuleLoader extends tc.oc.pgm.module.ModuleLoader<MatchModule> {

    private ModuleLoader() {
      super(getLogger());
    }

    @Override
    protected MatchModule loadModule(ModuleInfo info) throws ModuleLoadException {
      MatchModuleFactory<?> factory = getMap().getFactoryContext().getMatchModuleFactory(info);
      if (factory == null) throw new ModuleLoadException(info, "Missing match module factory");

      MatchModule matchModule = factory.createMatchModule(MatchImpl.this);
      if (!loadMatchModule(matchModule)) return null;

      return matchModule;
    }
  }

  private boolean loadMatchModule(MatchModule matchModule) throws ModuleLoadException {
    if (matchModule == null) return false;

    try {
      logger.fine("Loading " + matchModule.getClass().getSimpleName());

      if (context.load(matchModule)) {
        if (matchModule instanceof Listener && getListenerScope((Listener) matchModule) == null) {
          logger.warning(
              matchModule.getClass().getSimpleName()
                  + " implements Listener but is not annotated with @ListenerScope");
        }

        // TODO: Make default scope for listeners/tickables more consistent
        if (matchModule instanceof Listener) {
          addListener(
              (Listener) matchModule, getListenerScope((Listener) matchModule, MatchScope.RUNNING));
        }

        // TODO: Allow tickable scope to be specified with an annotation
        if (matchModule instanceof Tickable) {
          addTickable((Tickable) matchModule, MatchScope.LOADED);
        }
        return true;

      } else {
        logger.fine("Module " + matchModule.getClass().getSimpleName() + " declined to load");
        return false;
      }
    } catch (ModuleLoadException e) {
      e.fillInModule(ModuleInfo.get(matchModule.getClass()));
      throw e;
    }
  }

  @Override
  public void load() throws ModuleLoadException {
    try {
      ModuleLoader loader = new ModuleLoader();

      logger.fine("Loading static match modules...");
      if (!loader.loadAll(getMap().getFactoryContext().getMatchModules(), true)) {
        // If loading fails, rethrow the first exception, which should be the only one
        throw loader.getErrors().iterator().next();
      }

      logger.fine("Loading map modules...");
      for (MapModule module : getMapContext().getModules()) {
        MatchModule matchModule = module.createMatchModule(this);
        if (matchModule != null) {
          loadMatchModule(matchModule);
          loader.addModule(matchModule);
        }
      }

      for (Feature feature : getFeatureContext().getAll()) {
        if (feature instanceof Listener) {
          addListener((Listener) feature, getListenerScope((Listener) feature, MatchScope.RUNNING));
        }

        if (feature instanceof Tickable) {
          addTickable((Tickable) feature, MatchScope.LOADED);
        }
      }

      startListeners(MatchScope.LOADED);
      startTickables(MatchScope.LOADED);
      addParty(observers);

      loaded.set(true);
      callEvent(new MatchLoadEvent(this));
    } catch (Throwable e) {
      unload();
      throw e;
    }
  }

  @Override
  public void unload() {
    while (players.size() > 0) {
      removePlayer(Bukkit.getPlayer(players.keySet().iterator().next()));
    }

    if (loaded.getAndSet(false)) {
      callEvent(new MatchUnloadEvent(this));
    }

    if (parties.contains(observers)) {
      removeParty(observers);
    }

    getScheduler(MatchScope.RUNNING).cancel();
    getScheduler(MatchScope.LOADED).cancel();
    getCountdown().cancelAll();
    removeListeners(MatchScope.LOADED);

    for (MatchModule matchModule : getModuleContext().getAll()) {
      try {
        matchModule.unload();
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Exception unloading " + matchModule, e);
      }
    }

    Stream.of(getWorld().getLoadedChunks()).forEach(chunk -> chunk.unload(true, false));
    getWorld().getEntities().stream().forEach(Entity::remove);

    final boolean unloaded = PGM.get().getServer().unloadWorld(getWorld(), false);
    if (!unloaded) {
      logger.log(
          Level.SEVERE,
          "Unable to unload world " + getWorld().getName() + " (this can cause memory leaks!)");
    }

    schedulers.clear();
    listeners.clear();
    tickables.clear();
    players.clear();
    partyChanges.clear();
    parties.clear();
    victory.clear();
    competitors.clear();

    // TODO: Forcefully remove objects from the world like entities and chunls
    map.enqueue();
    world.enqueue();

    loaded.compareAndSet(true, false);
  }

  @Override
  public int compareTo(Match o) {
    return Comparator.comparing(Match::getDuration).thenComparing(Match::getId).compare(this, o);
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
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("map", getMap())
        .append("world", getWorld())
        .append("scope", getScope())
        .append("state", getPhase())
        .build();
  }
}
