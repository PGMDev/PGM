package tc.oc.pgm.match;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Supplier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Physical;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.chat.Audience;
import tc.oc.chat.CommandSenderAudience;
import tc.oc.chat.MultiAudience;
import tc.oc.chat.Sound;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.PGM;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.*;
import tc.oc.pgm.features.Feature;
import tc.oc.pgm.features.MatchFeatureContext;
import tc.oc.pgm.ffa.events.MatchResizeEvent;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.map.*;
import tc.oc.pgm.module.ModuleInfo;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.module.ModuleLoader;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.result.CompetitorVictoryCondition;
import tc.oc.pgm.result.VictoryCondition;
import tc.oc.pgm.time.TickClock;
import tc.oc.pgm.time.TickTime;
import tc.oc.pgm.time.WorldTickClock;
import tc.oc.pgm.util.TranslationUtils;
import tc.oc.server.Events;
import tc.oc.server.Scheduler;
import tc.oc.util.collection.ArrayUtils;
import tc.oc.util.collection.LinkedHashMultimap;
import tc.oc.util.collection.PunchClock;
import tc.oc.util.collection.RankedSet;
import tc.oc.util.components.ComponentUtils;
import tc.oc.util.logging.ClassLogger;
import tc.oc.world.NMSHacks;
import tc.oc.world.WorldTickRandom;

public class Match implements Audience {

  // Dependencies
  protected final ClassLogger logger;

  protected final Random random = new Random();
  protected final WorldTickRandom worldTickRandom;

  protected final PGM pgm;
  protected final PluginManager pm;
  protected final PGMMap map;
  protected final World world;

  protected final Audience consoleAudience;
  protected final Audience participantAudience;

  // Time
  protected final WorldTickClock clock;
  protected final Supplier<Duration> runningTimeSupplier =
      new Supplier<Duration>() {
        @Override
        public Duration get() {
          return getRunningTime();
        }
      };

  // Re-usable filter query
  protected final MatchQuery query = new MatchQuery(null, this);

  // Armor stand used to freeze players
  protected final int freezeEntityId = NMSHacks.allocateEntityId();

  // State management
  private final AtomicBoolean loaded = new AtomicBoolean();
  protected final TickTime loadTime;
  protected @Nullable TickTime unloadTime;

  protected MatchState state;
  protected final Map<MatchState, TickTime> stateTimeChange = Maps.newHashMap();
  protected @Nullable TickTime commitTime;

  // Contexts
  protected final SingleCountdownContext countdownContext;
  protected final MatchModuleContext matchModuleContext;

  // Player limit
  // TODO: could be provided by JoinHandlers
  protected int maxPlayers;

  // Parties
  protected final Observers observers;
  protected final Set<Party> parties = new HashSet<>();
  protected final Set<Competitor> pastCompetitors =
      new HashSet<>(); // All competitors who have played in the match at any time
  protected final LinkedHashMultimap<UUID, Competitor> pastCompetitorsByPlayer =
      new LinkedHashMultimap<>();

  private final Map<MatchPlayer, Party> partyChanges =
      new HashMap<>(); // Used to detect re-entrancy of the party change method

  // Victory
  protected NavigableSet<VictoryCondition> victoryConditions =
      new TreeSet<>(
          new Comparator<VictoryCondition>() {
            @Override
            public int compare(VictoryCondition a, VictoryCondition b) {
              return a.getPriority().compareTo(b.getPriority());
            }
          });

  class VictoryOrder implements Comparator<Competitor> {
    @Override
    public int compare(Competitor a, Competitor b) {
      for (VictoryCondition condition : victoryConditions) {
        int result = condition.compare(a, b);
        if (result != 0 || condition.isFinal(Match.this)) return result;
      }
      return 0;
    }
  }

  protected final VictoryOrder victoryOrder = new VictoryOrder();
  protected final RankedSet<Competitor> rankedCompetitors = new RankedSet<>(victoryOrder);

  // Players
  protected final Map<Player, MatchPlayer> players = new HashMap<>();
  protected final SetMultimap<Party.Type, MatchPlayer> playersByType = HashMultimap.create();
  protected final Set<UUID> pastParticipants = new HashSet<>();
  protected final PunchClock<UUID> participationClock = new PunchClock<>(getRunningTimeSupplier());

  // Events/ticking
  protected final SetMultimap<MatchScope, Listener> listeners = HashMultimap.create();
  protected final SetMultimap<MatchScope, Tickable> tickables = HashMultimap.create();

  // Record keeping
  protected final UUID uuid = UUID.randomUUID();
  protected final String id;

  // Scoped task schedulers
  protected final EnumMap<MatchScope, Scheduler> schedulers = new EnumMap<>(MatchScope.class);

  /** Construct a Match in the Idle state. Does not load any modules. */
  public Match(PGM pgm, PGMMap map, World world) {
    this.logger = ClassLogger.get(pgm.getLogger(), this.getClass());

    this.pgm = pgm;
    this.pm = pgm.getServer().getPluginManager();
    this.map = map;
    this.world = world;

    this.clock = new WorldTickClock(world);
    this.worldTickRandom = new WorldTickRandom(world, this.random);
    this.loadTime = this.clock.now();

    this.id = Long.toString(random.nextLong());

    this.consoleAudience = new CommandSenderAudience(pgm.getServer().getConsoleSender());
    this.participantAudience = (MultiAudience) this::getParticipatingPlayers;

    this.schedulers.put(MatchScope.LOADED, new Scheduler(pgm));
    this.schedulers.put(MatchScope.RUNNING, new Scheduler(pgm));

    this.countdownContext = new SingleCountdownContext(pgm, clock, logger);

    this.setState(MatchState.Idle);

    this.observers = new Observers(this);

    this.matchModuleContext = new MatchModuleContext(new MatchFeatureContext());
  }

  public static @Nullable Match get(World world) {
    if (world == null) return null;
    final MatchManager mm = PGM.getMatchManager();
    return mm == null ? null : mm.getMatch(world);
  }

  public static @Nullable Match get(Physical physical) {
    return physical == null ? null : get(physical.getWorld());
  }

  public static @Nullable Match get(Event event) {
    return event instanceof Physical ? get((Physical) event) : null;
  }

  public static Match get(CommandSender sender) {
    if (sender instanceof Physical) {
      return get((Physical) sender);
    } else {
      final MatchManager mm = PGM.getMatchManager();
      return mm == null ? null : mm.getCurrentMatch();
    }
  }

  public static Match need(World world) {
    return PGM.needMatchManager().needMatch(world);
  }

  public static Match need(Physical physical) {
    return get(physical.getWorld());
  }

  // -------------------
  // ---- Accessors ----
  // -------------------

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{world=" + this.getWorld().getName() + "}";
  }

  public ClassLogger getLogger() {
    return logger;
  }

  public String getId() {
    return id;
  }

  public UUID getUUID() {
    return this.uuid;
  }

  public PGMMap getMap() {
    return this.map;
  }

  public MapInfo getMapInfo() {
    return this.map.getInfo();
  }

  public PGM getPlugin() {
    return this.pgm;
  }

  public World getWorld() {
    return this.world;
  }

  public Server getServer() {
    return this.pgm.getServer();
  }

  public Audience getConsoleAudience() {
    return consoleAudience;
  }

  public PluginManager getPluginManager() {
    return this.getServer().getPluginManager();
  }

  public MatchQuery getQuery() {
    return query;
  }

  // -----------------------------
  // ---- Utility/Convenience ----
  // -----------------------------

  /** Allocate and return a unique entity ID from the server this match is on. */
  public int allocateEntityId() {
    return Bukkit.allocateEntityId();
  }

  /**
   * Get the match {@link Scheduler}. Any tasks scheduled through this will be automatically
   * cancelled when the match ends.
   */
  public Scheduler getScheduler(MatchScope scope) {
    return this.schedulers.get(scope);
  }

  // ---------------------
  // ---- World Clock ----
  // ---------------------

  public TickClock getClock() {
    return this.clock;
  }

  // ----------------
  // ---- Random ----
  // ----------------

  public Random getRandom() {
    return this.random;
  }

  /**
   * Return a random number in the range 0 <= n < 1 that is consistent for the duration of the
   * current tick for the given seed.
   */
  public double getRandomDoubleForTick(long seed) {
    return this.worldTickRandom.nextDouble(seed);
  }

  // ----------------
  // ---- Events ----
  // ----------------

  public void callEvent(Event event) {
    getPluginManager().callEvent(event);
  }

  public static @Nullable MatchScope getListenerScope(Listener thing) {
    return getListenerScope(thing, null);
  }

  public static MatchScope getListenerScope(Listener thing, MatchScope def) {
    ListenerScope listenerScope = thing.getClass().getAnnotation(ListenerScope.class);
    return listenerScope == null ? def : listenerScope.value();
  }

  public void registerEvents(Listener listener) {
    registerEvents(listener, MatchScope.LOADED);
  }

  /**
   * Register the given listener to receive events for this match. This consists of {@link
   * MatchEvent}s for this match plus all {@link Event}s that are not {@link MatchEvent}s.
   */
  public void registerEvents(Listener listener, MatchScope scope) {
    listeners.put(scope, listener);
    if (inScope(scope)) startListening(listener);
  }

  protected void startListening(MatchScope scope) {
    for (Listener listener : listeners.get(scope)) {
      startListening(listener);
    }
  }

  protected void startListening(Listener listener) {
    for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry :
        pgm.getPluginLoader().createRegisteredListeners(listener, pgm).entrySet()) {
      Class<? extends Event> eventClass = entry.getKey();
      HandlerList handlerList = Events.getEventListeners(eventClass);

      if (MatchEvent.class.isAssignableFrom(eventClass)) {
        for (final RegisteredListener registeredListener : entry.getValue()) {
          this.getPluginManager()
              .registerEvent(
                  eventClass,
                  listener,
                  registeredListener.getPriority(),
                  new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) throws EventException {
                      if (((MatchEvent) event).getMatch() == Match.this) {
                        registeredListener.callEvent(event);
                      }
                    }
                  },
                  pgm);
        }
      } else {
        handlerList.registerAll(entry.getValue());
      }
    }
  }

  protected void stopListening(MatchScope scope) {
    for (Listener listener : listeners.get(scope)) {
      HandlerList.unregisterAll(listener);
    }
  }

  public void registerTickable(Tickable tickable, MatchScope scope) {
    tickables.put(scope, tickable);
  }

  public void unregisterTickable(Tickable tickable) {
    for (MatchScope scope : MatchScope.values()) {
      tickables.remove(scope, tickable);
    }
  }

  // -----------------------------------
  // ---- Modules/Features/Contexts ----
  // -----------------------------------

  public ModuleRegistry getFactoryContext() {
    return this.map.getFactoryContext();
  }

  public MapModuleContext getModuleContext() {
    if (!this.map.getContext().isPresent()) {
      throw new IllegalStateException("Map is not loaded!");
    }
    return this.map.getContext().get();
  }

  public <T extends MatchModule> T getMatchModule(Class<T> matchModuleClass) {
    return this.matchModuleContext.getMatchModule(matchModuleClass);
  }

  public <T extends MatchModule> Iterable<T> getMatchModulesOfType(Class<T> matchModuleClass) {
    return this.matchModuleContext.getMatchModulesOfType(matchModuleClass);
  }

  public boolean hasMatchModule(Class<? extends MatchModule> matchModuleClass) {
    return getMatchModule(matchModuleClass) != null;
  }

  public <T extends MatchModule> T needMatchModule(Class<T> matchModuleClass) {
    T mm = getMatchModule(matchModuleClass);
    if (mm == null) {
      throw new IllegalStateException(
          "Required module " + matchModuleClass.getSimpleName() + " is not loaded");
    }
    return mm;
  }

  public SingleCountdownContext getCountdownContext() {
    return this.countdownContext;
  }

  public <T extends Feature> T getFeature(String id, Class<T> type) {
    return this.getMatchFeatureContext().get(id, type);
  }

  public Feature getFeature(String id) {
    return this.getMatchFeatureContext().get(id);
  }

  public MatchFeatureContext getMatchFeatureContext() {
    return this.matchModuleContext.getMatchFeatureContext();
  }

  // ---------------------
  // ---- Load/Unload ----
  // ---------------------

  /**
   * True if this Match is loaded. This is only set true after the entire loading process is
   * complete i.e. all modules are loaded, events are called etc. Likewise, it is set false before
   * the unloading process starts. It is safe to call this method from any thread.
   */
  public boolean isLoaded() {
    return this.loaded.get();
  }

  public Instant getLoadTime() {
    return loadTime.instant;
  }

  public @Nullable Instant getUnloadTime() {
    return unloadTime == null ? null : unloadTime.instant;
  }

  private class MatchModuleLoader extends ModuleLoader<MatchModule> {
    protected MatchModuleLoader() {
      super(getLogger());
    }

    @Override
    protected MatchModule loadModule(ModuleInfo info) throws ModuleLoadException {
      MatchModuleFactory<?> factory = getFactoryContext().getMatchModuleFactory(info);
      if (factory == null) throw new ModuleLoadException(info, "Missing factory");

      MatchModule matchModule = factory.createMatchModule(Match.this);
      if (matchModule == null) return null;

      if (!loadMatchModule(matchModule)) return null;

      return matchModule;
    }
  }

  private boolean loadMatchModule(MatchModule matchModule) throws ModuleLoadException {
    if (matchModule == null) return false;

    try {
      logger.fine("Loading " + matchModule.getClass().getSimpleName());

      if (matchModuleContext.load(matchModule)) {
        if (matchModule instanceof Listener && getListenerScope((Listener) matchModule) == null) {
          logger.warning(
              matchModule.getClass().getSimpleName()
                  + " implements Listener but is not annotated with @ListenerScope");
        }

        // TOOD: Make default scope for listeners/tickables more consistent
        if (matchModule instanceof Listener) {
          registerEvents(
              (Listener) matchModule, getListenerScope((Listener) matchModule, MatchScope.RUNNING));
        }

        // TODO: allow tickable scope to be specified with an annotation
        if (matchModule instanceof Tickable) {
          registerTickable((Tickable) matchModule, MatchScope.LOADED);
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

  public void load() throws ModuleLoadException {
    try {
      MatchModuleLoader loader = new MatchModuleLoader();

      // Load static registered MatchModules
      logger.fine("Loading static match modules...");
      if (!loader.loadAll(getFactoryContext().getMatchModules(), true)) {
        // If loading fails, rethrow the first exception, which should be the only one
        throw loader.getErrors().iterator().next();
      }

      // Load dynamic MatchModules created by MapModules
      logger.fine("Loading map modules...");
      for (MapModule module : getModuleContext().getModules()) {
        MatchModule matchModule = module.createMatchModule(this);
        if (matchModule != null) {
          loadMatchModule(matchModule);
          loader.addModule(matchModule);
        }
      }

      for (Feature feature : this.getMatchFeatureContext().getAll()) {
        if (feature instanceof Listener) {
          registerEvents(
              (Listener) feature, getListenerScope((Listener) feature, MatchScope.RUNNING));
        }

        if (feature instanceof Tickable) {
          registerTickable((Tickable) feature, MatchScope.LOADED);
        }
      }

      startListening(MatchScope.LOADED);

      getScheduler(MatchScope.LOADED).runTaskTimer(1, new TickableTask(MatchScope.LOADED));

      addParty(observers);

      this.getPluginManager().callEvent(new MatchLoadEvent(this));

      this.loaded.set(true);

    } catch (Throwable e) {
      unload();
      throw e;
    }
  }

  public void unload() {
    checkState(this.getPlayers().isEmpty(), "cannot unload a match with players");

    boolean wasLoaded = this.loaded.get();
    this.loaded.set(false);
    this.unloadTime = getClock().now();

    if (wasLoaded) {
      this.getPluginManager().callEvent(new MatchUnloadEvent(this));
    }

    if (parties.contains(observers)) {
      removeParty(observers);
    }

    this.schedulers.get(MatchScope.RUNNING).cancel();
    this.schedulers.get(MatchScope.LOADED).cancel();
    this.countdownContext.cancelAll();

    stopListening(MatchScope.LOADED);

    for (MatchModule matchModule : this.matchModuleContext.getAll()) {
      try {
        matchModule.unload();
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Exception unloading " + matchModule, e);
      }
    }
  }

  // -----------------
  // ---- Victory ----
  // -----------------

  public Comparator<Competitor> getCompetitorRanking() {
    return victoryOrder;
  }

  public NavigableSet<VictoryCondition> getVictoryConditions() {
    return victoryConditions;
  }

  public void addVictoryCondition(VictoryCondition condition) {
    if (victoryConditions.add(condition)) {
      logger.fine("Added victory condition " + condition);
      invalidateCompetitorRanking();
    }
  }

  public void removeVictoryCondition(VictoryCondition condition) {
    if (victoryConditions.remove(condition)) {
      logger.fine("Removed victory condition " + condition);
      invalidateCompetitorRanking();
    }
  }

  public void removeVictoryConditions(Class<? extends VictoryCondition> klass) {
    boolean changed = false;
    for (Iterator<VictoryCondition> iterator = victoryConditions.iterator(); iterator.hasNext(); ) {
      VictoryCondition condition = iterator.next();
      if (klass.isInstance(condition)) {
        logger.fine("Removed victory condition " + condition);
        iterator.remove();
        changed = true;
      }
    }
    if (changed) invalidateCompetitorRanking();
  }

  /**
   * Re-sort all {@link Competitor}s from scratch, according to the current {@link
   * VictoryCondition}. This should be called when the state of the match changes in a way that
   * affects the rank of any competitors.
   */
  public void invalidateCompetitorRanking() {
    rankedCompetitors.invalidateRanking();
  }

  /**
   * Return all currently active competitors, ordered by closeness to winning the match. Competitors
   * that are tied are returned in arbitrary, and inconsistent order.
   */
  public RankedSet<Competitor> getRankedCompetitors() {
    return rankedCompetitors;
  }

  /**
   * Return all {@link Competitor}s that are as close or closer to winning the match than any other
   * competitor.
   */
  public Collection<Competitor> getLeaders() {
    return rankedCompetitors.getRank(0);
  }

  // ----------------
  // ---- States ----
  // ----------------

  public boolean isCommitted() {
    return commitTime != null;
  }

  public @Nullable TickTime getCommitTime() {
    return commitTime;
  }

  /**
   * Commit the match, if it is not already committed. Commitment is a boolean state that starts
   * false and becomes true at some point before or at match start. The transition only happens once
   * per match, and is irreversible, even if the start countdown is cancelled.
   *
   * <p>The commitment event is when teams are chosen/balanced (depending on settings), and also
   * when players become committed to playing the match, if that is enabled. If mid-match join is
   * disallowed, this is also when that restriction becomes effective.
   *
   * <p>Commitment happens automatically at match start, if this method has not been called before
   * then.
   */
  public void commit() {
    if (!isCommitted()) {
      callEvent(new MatchPreCommitEvent(this));

      commitTime = getClock().now();

      for (MatchPlayer player : getParticipatingPlayers()) {
        pastParticipants.add(player.getPlayerId());
        pastCompetitorsByPlayer.put(player.getPlayerId(), player.getCompetitor());
        player.commit();
      }

      for (Competitor competitor : getCompetitors()) {
        competitor.commit();
      }

      callEvent(new MatchPostCommitEvent(this));
    }
  }

  public MatchState getState() {
    return this.state;
  }

  public boolean inState(MatchState state) {
    return getState() == state;
  }

  public boolean inScope(MatchScope scope) {
    switch (scope) {
      case LOADED:
        return isLoaded();
      case RUNNING:
        return isRunning();
      default:
        throw new IllegalStateException();
    }
  }

  public boolean isStarting() {
    return inState(MatchState.Starting);
  }

  public boolean isRunning() {
    return inState(MatchState.Running);
  }

  public boolean isFinished() {
    return inState(MatchState.Finished);
  }

  public boolean hasStarted() {
    return inState(MatchState.Running) || inState(MatchState.Finished);
  }

  public boolean canTransitionTo(MatchState state) {
    return this.state == null || this.state.canTransitionTo(state);
  }

  public boolean setState(MatchState newState) {
    if (this.canTransitionTo(newState)) {
      if (newState == MatchState.Running) {
        commit();
      }

      MatchState oldState = this.state;
      this.state = newState;
      this.stateTimeChange.put(newState, this.clock.now());

      switch (newState) {
        case Running:
          onStart();
          break;

        case Finished:
          onEnd();
          break;
      }

      callEvent(new MatchStateChangeEvent(this, oldState, newState));

      return true;
    } else {
      return false;
    }
  }

  protected void onStart() {
    for (MatchModule matchModule : this.matchModuleContext.getAll()) {
      matchModule.enable();
    }

    startListening(MatchScope.RUNNING);
    getScheduler(MatchScope.RUNNING).runTaskTimer(1, new TickableTask(MatchScope.RUNNING));

    callEvent(new MatchBeginEvent(this));
    refreshPlayerGameModes();
  }

  public boolean start() {
    return setState(MatchState.Running);
  }

  // ----------------
  // ---- Ending ----
  // ----------------

  protected void onEnd() {
    invalidateCompetitorRanking();
    Collection<Competitor> winners = getLeaders();

    this.schedulers.get(MatchScope.RUNNING).cancel();
    this.getCountdownContext().cancelAll();

    this.callEvent(new MatchEndEvent(this, winners));

    stopListening(MatchScope.RUNNING);

    for (MatchModule matchModule : this.matchModuleContext.getAll()) {
      matchModule.disable();
    }

    this.refreshPlayerGameModes();
  }

  /** Immediately end the match with an unconditional victory for the given competitor. */
  public boolean end(@Nullable Competitor winner) {
    if (winner != null) this.addVictoryCondition(new CompetitorVictoryCondition(winner));
    return this.end();
  }

  /** Immediately end the match with a victory for the currently leading competitor. */
  public boolean end() {
    return setState(MatchState.Finished);
  }

  public boolean checkEnd() {
    if (isFinished()) return true;
    if (!hasStarted()) return false;

    logger.fine("Checking for match end");
    for (VictoryCondition condition : victoryConditions) {
      logger.fine("Checking victory condition " + condition);
      if (condition.isCompleted(this)) {
        logger.fine("Condition " + condition + " is satisfied, ending match");
        end();
        return true;
      }
    }
    return false;
  }

  // ---------------
  // ---- Times ----
  // ---------------

  public Instant getStateChangeTime(MatchState state) {
    TickTime time = this.stateTimeChange.get(state);
    return time == null ? null : time.instant;
  }

  // Get the time the match ended, or now if it's still running.
  // If the match hasn't started, return null.
  public @Nullable Instant getEndTime() {
    if (this.isFinished()) {
      return this.getStateChangeTime(MatchState.Finished);
    } else if (this.hasStarted()) {
      return this.getClock().now().instant;
    } else {
      return null;
    }
  }

  /**
   * Get the duration of the match
   *
   * @throws java.lang.IllegalArgumentException if the match has not started
   */
  public Duration getLength() {
    Instant startTime = this.getStateChangeTime(MatchState.Running);
    if (startTime == null) {
      throw new IllegalArgumentException("match has not started yet");
    }
    return new Duration(startTime, this.getEndTime());
  }

  /** Get the duration of the match, or zero if the match has not started */
  public Duration getRunningTime() {
    Instant startTime = this.getStateChangeTime(MatchState.Running);
    if (startTime == null) {
      return Duration.ZERO;
    }
    return new Duration(startTime, this.getEndTime());
  }

  public Supplier<Duration> getRunningTimeSupplier() {
    return runningTimeSupplier;
  }

  public PunchClock<UUID> getParticipationClock() {
    return participationClock;
  }

  // -----------------
  // ---- Players ----
  // -----------------

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    if (this.maxPlayers != maxPlayers) {
      this.maxPlayers = maxPlayers;
      callEvent(new MatchResizeEvent(this));
    }
  }

  public Collection<MatchPlayer> getPlayers() {
    return this.players.values();
  }

  /** Players who have been in a participating party after match commitment */
  public Set<UUID> getPastParticipants() {
    return pastParticipants;
  }

  public boolean hasEverParticipated(UUID playerId) {
    return getPastParticipants().contains(playerId);
  }

  public Set<MatchPlayer> getPlayers(Party.Type type) {
    return playersByType.get(type);
  }

  public Set<MatchPlayer> getObservingPlayers() {
    return playersByType.get(Party.Type.Observing);
  }

  public Set<MatchPlayer> getParticipatingPlayers() {
    return playersByType.get(Party.Type.Participating);
  }

  public Audience getParticipantAudience() {
    return participantAudience;
  }

  /**
   * If the given Player is non-null and has joined this match, return their MatchPlayer, otherwise
   * return null.
   */
  public @Nullable MatchPlayer getPlayer(@Nullable Player bukkit) {
    return bukkit == null ? null : this.players.get(bukkit);
  }

  public @Nullable MatchPlayer getPlayer(@Nullable Entity bukkit) {
    return bukkit instanceof Player ? this.getPlayer((Player) bukkit) : null;
  }

  public @Nullable MatchPlayer getPlayer(@Nullable CommandSender bukkit) {
    return bukkit instanceof Player ? this.getPlayer((Player) bukkit) : null;
  }

  public @Nullable MatchPlayer getPlayer(@Nullable UUID id) {
    return getPlayer(Bukkit.getPlayer(id));
  }

  public @Nullable MatchPlayer getParticipant(@Nullable Entity bukkit) {
    if (!(bukkit instanceof Player)) return null;
    MatchPlayer player = players.get(bukkit);
    return player != null && player.isParticipating() ? player : null;
  }

  public @Nullable ParticipantState getParticipantState(@Nullable Player bukkit) {
    MatchPlayer player = getPlayer(bukkit);
    return player == null ? null : player.getParticipantState();
  }

  public @Nullable ParticipantState getParticipantState(@Nullable Entity bukkit) {
    return bukkit instanceof Player ? getParticipantState((Player) bukkit) : null;
  }

  public boolean canInteract(@Nullable MatchPlayer player) {
    return player != null && player.canInteract();
  }

  public boolean canInteract(@Nullable Player bukkit) {
    return canInteract(getPlayer(bukkit));
  }

  public boolean canInteract(@Nullable Entity bukkit) {
    return !(bukkit instanceof Player)
        || canInteract((Player) bukkit); // Assume all non-player entities can interact
  }

  public boolean canInteract(@Nullable MatchPlayerState player) {
    return player != null && player.canInteract();
  }

  public MatchPlayer addPlayer(Player bukkit) {
    MatchPlayer player = this.players.get(bukkit);
    if (player == null) {
      logger.fine("Adding player " + bukkit);

      MatchPlayerAddEvent event = new MatchPlayerAddEvent(this, bukkit, getDefaultParty());
      callEvent(event);

      player = new MatchPlayer(bukkit, this);
      setPlayerParty(player, event.getInitialParty());
    }
    return player;
  }

  public void removePlayer(Player bukkit) {
    MatchPlayer player = this.players.get(bukkit);
    if (player != null) {
      logger.fine("Removing player " + player);
      setOrClearPlayerParty(player, null);
    }
  }

  public void removeAllPlayers() {
    while (this.players.size() > 0) {
      this.removePlayer(this.players.keySet().iterator().next());
    }
  }

  /** Refreshes the visibility and game modes for everyone in this match. */
  public void refreshPlayerGameModes() {
    for (MatchPlayer player : this.players.values()) {
      player.refreshGameMode();
    }
  }

  // -----------------
  // ---- Parties ----
  // -----------------

  /** Return all currently active parties */
  public Set<Party> getParties() {
    return parties;
  }

  public Collection<Competitor> getCompetitors() {
    return rankedCompetitors;
  }

  /** Return all competitors who have ever participated in the match */
  public Collection<Competitor> getPastCompetitors() {
    return pastCompetitors;
  }

  /** Return all competitors that have ever included the given player after match commitment */
  public Set<Competitor> getPastCompetitors(UUID playerId) {
    return pastCompetitorsByPlayer.get(playerId);
  }

  public @Nullable Competitor getLastCompetitor(UUID playerId) {
    return Iterables.getLast(getPastCompetitors(playerId), null);
  }

  public void addParty(Party party) {
    logger.fine("Adding party " + party);
    checkNotNull(party);
    checkState(party.getPlayers().isEmpty(), "Party already contains players");
    checkState(parties.add(party), "Party is already in this match");

    if (party instanceof Competitor) {
      rankedCompetitors.add((Competitor) party);
      pastCompetitors.add((Competitor) party);
    }

    callEvent(
        party instanceof Competitor
            ? new CompetitorAddEvent((Competitor) party)
            : new PartyAddEvent(party));
  }

  public void removeParty(Party party) {
    logger.fine("Removing party " + party);

    checkNotNull(party);
    checkState(parties.contains(party), "Party is not in this match");
    checkState(party.getPlayers().isEmpty(), "Party still has players in it");

    callEvent(
        party instanceof Competitor
            ? new CompetitorRemoveEvent((Competitor) party)
            : new PartyRemoveEvent(party));

    if (party instanceof Competitor) rankedCompetitors.remove(party);
    parties.remove(party);
  }

  public Party getDefaultParty() {
    return observers;
  }

  public boolean setPlayerParty(MatchPlayer player, Party newParty) {
    return setOrClearPlayerParty(player, checkNotNull(newParty));
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
    Party oldParty = player.party;

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
        this.players.put(player.getBukkit(), player);
        registerTickable(player, MatchScope.LOADED);
      } else {
        // Player is leaving a party
        if (newParty == null) {
          // If they are not joining a new party, they are also leaving the match
          callEvent(new PlayerLeaveMatchEvent(player, oldParty));
        } else {
          callEvent(new PlayerLeavePartyEvent(player, oldParty));
        }

        // Update the old party's state
        oldParty.removePlayer(player);
        playersByType.remove(player.party.getType(), player);

        if (player.party instanceof Competitor) {
          getParticipationClock().punchOut(player.getPlayerId());
        }
      }

      // Update the player's state
      player.setParty(newParty);

      if (newParty == null) {
        // Player is leaving the match, remove them before calling the event.
        // Passing an orphan player to the event is probably safer than leaving them in
        // the match with a null party. Anything that needs to be called before the player
        // is removed should listen for PlayerMatchLeaveEvent.
        unregisterTickable(player);
        this.players.remove(player.getBukkit());

        callEvent(new PlayerPartyChangeEvent(player, oldParty, null));
      } else {
        // Player is joining a party
        // Update the new party's state
        playersByType.put(player.party.getType(), player);
        if (newParty instanceof Competitor) {
          getParticipationClock().punchIn(player.getPlayerId());

          if (isCommitted()) {
            pastCompetitorsByPlayer.force(player.getPlayerId(), (Competitor) newParty);
            if (pastParticipants.add(player.getPlayerId())) {
              player.commit();
            }
          }
        }
        newParty.addPlayer(player);

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

  // --------------
  // ---- Chat ----
  // --------------

  private static final int CHAT_WIDTH = 200;

  public void sendWelcomeMessage(MatchPlayer viewer) {
    MapInfo mapInfo = this.getMapInfo();

    String title = ChatColor.AQUA.toString() + ChatColor.BOLD + mapInfo.name;
    viewer.sendMessage(ComponentUtils.horizontalLineHeading(title, ChatColor.WHITE, CHAT_WIDTH));

    String objective = " " + ChatColor.BLUE + ChatColor.ITALIC + mapInfo.objective;
    viewer.sendMessage(ComponentUtils.wordWrap(objective, CHAT_WIDTH));

    List<Contributor> authors = mapInfo.getNamedAuthors();
    if (!authors.isEmpty()) {
      viewer.sendMessage(
          new PersonalizedText(" ", ChatColor.DARK_GRAY)
              .extra(
                  new PersonalizedTranslatable(
                      "broadcast.welcomeMessage.createdBy",
                      TranslationUtils.nameList(NameStyle.FANCY, authors))));
    }

    viewer.sendMessage(ComponentUtils.horizontalLine(ChatColor.WHITE, CHAT_WIDTH));
  }

  @Override
  public void sendMessage(Component message) {
    consoleAudience.sendMessage(message);
    for (MatchPlayer player : this.players.values()) {
      player.sendMessage(message);
    }
  }

  @Override
  public void sendMessage(String message) {
    consoleAudience.sendMessage(message);
    for (MatchPlayer player : this.players.values()) {
      player.sendMessage(message);
    }
  }

  @Override
  public void sendWarning(Component message, boolean audible) {
    consoleAudience.sendWarning(message, audible);
    for (MatchPlayer player : this.players.values()) {
      player.sendWarning(message, audible);
    }
  }

  @Override
  public void sendWarning(String message, boolean audible) {
    consoleAudience.sendWarning(message, audible);
    for (MatchPlayer player : this.players.values()) {
      player.sendWarning(message, audible);
    }
  }

  public void sendMessageExcept(Component message, MatchPlayer... except) {
    consoleAudience.sendMessage(message);
    for (MatchPlayer player : this.players.values()) {
      if (!ArrayUtils.contains(except, player)) {
        player.sendMessage(message);
      }
    }
  }

  public void sendMessageExcept(Component message, MatchPlayerState... except) {
    consoleAudience.sendMessage(message);
    players:
    for (MatchPlayer player : this.players.values()) {
      for (MatchPlayerState anExcept : except) {
        if (anExcept.isPlayer(player)) continue players;
      }
      player.sendMessage(message);
    }
  }

  @Override
  public void playSound(Sound sound) {
    consoleAudience.playSound(sound);
    for (MatchPlayer player : this.players.values()) {
      player.playSound(sound);
    }
  }

  @Override
  public void sendHotbarMessage(Component message) {
    consoleAudience.sendHotbarMessage(message);
    for (MatchPlayer player : this.players.values()) {
      player.sendHotbarMessage(message);
    }
  }

  @Override
  public void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks) {
    consoleAudience.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
    for (MatchPlayer player : this.players.values()) {
      player.showTitle(title, subtitle, inTicks, stayTicks, outTicks);
    }
  }

  class TickableTask implements Runnable {
    final MatchScope scope;

    TickableTask(MatchScope scope) {
      this.scope = scope;
    }

    @Override
    public void run() {
      for (Tickable tickable : tickables.get(scope)) {
        tickable.tick(Match.this);
      }
    }
  }
}
