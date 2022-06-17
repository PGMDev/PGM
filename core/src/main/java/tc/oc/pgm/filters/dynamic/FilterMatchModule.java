package tc.oc.pgm.filters.dynamic;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.Filters;
import tc.oc.pgm.filters.ParentFilter;
import tc.oc.pgm.filters.XMLFilterReference;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.util.MapUtils;
import tc.oc.pgm.util.MethodHandleUtils;
import tc.oc.pgm.util.collection.ContextStore;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/**
 * Handler of dynamic filter magic.
 *
 * <p>A {@link Filter} can have the possibility of being dynamic or not, but the match module
 * decides whether to use a filter dynamically or not. In this sense the docs can be a little
 * misleading when thinking about dynamic filters programmatically. The docs say "the [dynamic]
 * filter will notify the module when it's time to do something". In reality this {@link
 * FilterMatchModule} will "notify" the modules using registered {@link FilterListener}s to execute
 * code in the module.
 *
 * <p>When registering {@link FilterListener}s we use the words "rise" and "fall" to describe the
 * states of a dynamic filter we want to listen for. The relevant methods are explained in {@link
 * FilterDispatcher}.
 *
 * @see #onChange(Class, Filter, FilterListener)
 * @see #onRise(Class, Filter, Consumer)
 * @see #onFall(Class, Filter, Consumer)
 */
@ListenerScope(MatchScope.LOADED)
public class FilterMatchModule implements MatchModule, FilterDispatcher, Tickable, Listener {

  private final Match match;
  private final ContextStore<?> filterContext;
  private final List<Class<? extends Event>> listeningFor = new LinkedList<>();

  private final Map<ReactorFactory<?>, ReactorFactory.Reactor> activeReactors = new HashMap<>();

  private final DummyListener dummyListener = new DummyListener();

  /**
   * Create the FilterMatchModule
   *
   * @param match the match this module exists in
   * @param filterContext the context where all {@link Filters} for the relevant match can be found.
   *     Important to find {@link ReactorFactory}s
   */
  public FilterMatchModule(Match match, ContextStore<?> filterContext) {
    this.match = match;
    this.filterContext = filterContext;
  }

  private static class ListenerSet {
    final Set<FilterListener<?>> rise = new HashSet<>();
    final Set<FilterListener<?>> fall = new HashSet<>();
  }

  private final Table<Filter, Class<? extends Filterable<?>>, ListenerSet> listeners =
      HashBasedTable.create();

  // Most recent responses for each filter with listeners (used to detect changes)
  private final Table<Filter, Filterable<?>, Boolean> lastResponses = HashBasedTable.create();

  // Filterables that need a check in the next tick (cleared every tick)
  private final Set<Filterable<?>> dirtySet = new HashSet<>();

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {

    // These two have to be on separate passes, in case a reactor wants to register a child filter
    // for dynamic listening. This could potentially result in a need to register a specific event
    // listener.
    this.filterContext // Check for any factories hidden in filter trees
        .getAllUnchecked(Filter.class)
        .forEach(this::findAndCreateReactorFactories);

    // Then register all event listeners
    this.listeners
        .rowKeySet()
        .forEach(
            filter -> {
              if (filter.getRelevantEvents().isEmpty()) {
                match
                    .getLogger()
                    .warning("Filter " + filter + " was submitted as a dynamic filter but is not!");
                return;
              }
              this.registerListenersFor(filter.getRelevantEvents());
            });

    // Lastly dispatch initial states of all dynamic filters for the relevant scopes
    // Has to be last since many filters depend on objects loaded in a non-consequent way during
    // match load e.g. the goals
    for (Filter filter : this.listeners.rowKeySet()) {
      Map<Class<? extends Filterable<?>>, ListenerSet> row = this.listeners.row(filter);
      for (Class<? extends Filterable<?>> scope : Filter.SCOPES) {
        ListenerSet set = row.get(scope);
        if (set == null) continue;
        match
            .getFilterableDescendants(scope)
            .forEach(
                filterable -> {
                  final boolean last = this.lastResponse(filter, filterable);
                  if (!last) {
                    for (FilterListener<?> filterListener : set.fall) {
                      dispatch(
                          (FilterListener<Filterable<?>>) filterListener, filter, filterable, last);
                    }
                  } else {
                    for (FilterListener<?> filterListener : set.rise) {
                      dispatch(
                          (FilterListener<Filterable<?>>) filterListener, filter, filterable, last);
                    }
                  }
                });
      }
    }
  }

  private void findAndCreateReactorFactories(Filter filter) {
    if (filter instanceof XMLFilterReference) {
      this.findAndCreateReactorFactories(((XMLFilterReference) filter).get());
      return;
    }
    if (filter instanceof ParentFilter) {
      ((ParentFilter) filter).getChildren().forEach(this::findAndCreateReactorFactories);
    }
    if (filter instanceof ReactorFactory) {
      ReactorFactory<? extends ReactorFactory.Reactor> factory =
          (ReactorFactory<? extends ReactorFactory.Reactor>) filter;
      activeReactors.computeIfAbsent(factory, f -> f.createReactor(match, this));
    }
  }

  @Override
  public void unload() {
    HandlerList.unregisterAll(this.dummyListener);
    this.activeReactors.values().forEach(ReactorFactory.Reactor::unload);
  }

  /**
   * Registers a filter listener for the given scope to be notified when the response of the
   * provided filter is equal to the provided response.
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param response The desired response
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  private <F extends Filterable<?>> void register(
      Class<F> scope, Filter filter, boolean response, FilterListener<? super F> listener) {
    if (match.isRunning()) {
      throw new IllegalStateException("Cannot register filter listener after match has started");
    }

    final ListenerSet listenerSet =
        this.listeners.row(filter).computeIfAbsent(scope, s -> new ListenerSet());

    (response ? listenerSet.rise : listenerSet.fall).add(listener);
  }

  /**
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public <F extends Filterable<?>> void onChange(
      Class<F> scope, Filter filter, FilterListener<? super F> listener) {
    match
        .getLogger()
        .fine(
            "onChange scope="
                + scope.getSimpleName()
                + " listener="
                + listener
                + " filter="
                + filter);
    register(scope, filter, true, listener);
    register(scope, filter, false, listener);
  }

  /**
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public void onChange(Filter filter, FilterListener<? super Filterable<?>> listener) {
    onChange((Class) Filterable.class, filter, listener);
  }

  /**
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public <F extends Filterable<?>> void onRise(
      Class<F> scope, Filter filter, Consumer<? super F> listener) {
    match
        .getLogger()
        .fine(
            "onRise scope="
                + scope.getSimpleName()
                + " listener="
                + listener
                + " filter="
                + filter);
    register(scope, filter, true, (filterable, response) -> listener.accept(filterable));
  }

  /**
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public void onRise(Filter filter, Consumer<? super Filterable<?>> listener) {
    onRise((Class) Filterable.class, filter, listener);
  }

  /**
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public <F extends Filterable<?>> void onFall(
      Class<F> scope, Filter filter, Consumer<? super F> listener) {
    match
        .getLogger()
        .fine(
            "onFall scope="
                + scope.getSimpleName()
                + " listener="
                + listener
                + " filter="
                + filter);
    register(scope, filter, false, (filterable, response) -> listener.accept(filterable));
  }

  /**
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   * @throws IllegalStateException if the match is running at register time
   */
  @Override
  public void onFall(Filter filter, Consumer<? super Filterable<?>> listener) {
    onFall((Class) Filterable.class, filter, listener);
  }

  /** Returns the last response a given filter gave to a given filterable */
  private boolean lastResponse(Filter filter, Filterable<?> filterable) {
    return MapUtils.computeIfAbsent(this.lastResponses.row(filter), filterable, filter::response);
  }

  /**
   * Dispatches a response to a filter listener.
   *
   * @param listener the listener to send an update to
   * @param filter the filter the update came from
   * @param filterable the filterable the update is about
   * @param response true -> rise, false -> fall
   */
  private <F extends Filterable<?>> void dispatch(
      FilterListener<? super F> listener, Filter filter, F filterable, boolean response) {
    if (match.getLogger().isLoggable(Level.FINER)) {
      match
          .getLogger()
          .finer(
              "Dispatching response="
                  + response
                  + " listener="
                  + listener
                  + " filter="
                  + filter
                  + " filterable="
                  + filterable);
    }
    listener.filterQueryChanged(filterable, response);
  }

  /**
   * Checks the response for a query for all filters that fits a scope, if any response is different
   * than the last cached response and the filter cares about the change a runnable where the
   * dispatching of the new response is done is created and stored in the provided list.
   *
   * @param filterable the scope for this check
   * @param query the query to check against some filters
   * @param dispatches will get a runnable for each matching filter that has a new response
   */
  private <F extends Filterable<?>, Q extends Query> void check(
      F filterable, Q query, List<Runnable> dispatches) {
    final Map<Filter, Boolean> beforeCache = new HashMap<>();
    final Map<Filter, Boolean> afterCache = this.lastResponses.column(filterable);

    // For each scope that the given filterable applies to
    this.listeners
        .columnMap()
        .forEach(
            (scope, column) -> {
              if (scope.isInstance(filterable)) {
                // For each filter in this scope
                column.forEach(
                    (filter, filterListeners) -> {
                      final Boolean before;
                      final boolean after;
                      if (beforeCache.containsKey(filter)) {
                        // If the filter has already been checked, we have both responses saved.
                        before = beforeCache.get(filter);
                        after = afterCache.get(filter);
                      } else {
                        // The first time a particular filter is checked, move the old response to
                        // a local temporary cache and save the new response to the permanent cache.
                        before = afterCache.get(filter);
                        beforeCache.put(filter, before);
                        after = filter.response(query);
                        afterCache.put(filter, after);
                      }

                      if (before == null || before != after) {
                        dispatches.add(
                            () ->
                                (after ? filterListeners.rise : filterListeners.fall)
                                    .forEach(
                                        listener ->
                                            dispatch(
                                                (FilterListener<? super F>) listener,
                                                filter,
                                                filterable,
                                                after)));
                      }
                    });
              }
            });
  }

  /**
   * Checks the response for a query for all filters that fits a scope. If the response is different
   * from the last cached response and the filters cares about the change the responses are
   * dispatched to all the filters after the check is done.
   *
   * @param filterable the scope for this check
   * @param query the query to check against some filters
   */
  private <F extends Filterable<?>, Q extends Query> void check(F filterable, Q query) {
    final List<Runnable> dispatches = new ArrayList<>();
    check(filterable, query, dispatches);
    dispatches.forEach(Runnable::run);
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!match.isRunning()) return;

    this.tick();
  }

  public void tick() {
    final Set<Filterable<?>> checked = new HashSet<>();
    Set<Filterable<?>> checking;
    // Collect Filterables that are dirty, and have not already been checked in this tick
    while (!(checking = ImmutableSet.copyOf(Sets.difference(dirtySet, checked))).isEmpty()) {
      // Remove what we are about to check from the dirty set, and add them to the checked set
      dirtySet.removeAll(checking);
      checked.addAll(checking);

      // Do all the filter checks and collect the notifications in a list to dispatch afterward.
      // This prevents listeners from altering the results of filters for other listeners that
      // were invalidated at the same time.
      final List<Runnable> dispatches = new ArrayList<>();
      checking.forEach(f -> check(f, f, dispatches));

      // The Listeners might invalidate more Filterables, which is why we have to loop around
      // and empty the dirtySet again after this. We keep looping until there is nothing more
      // we can check in this tick. If they invalidate something that has already been checked
      // in this tick, it will remain in the dirtySet until the next tick.
      dispatches.forEach(Runnable::run);
    }
  }

  public void invalidate(Filterable<?> filterable) {
    if (dirtySet.add(filterable)) {
      filterable.getFilterableChildren().forEach(this::invalidate);
    }
  }

  /** TODO: optimize using the filter parameter */
  public void invalidate(Filter filter, Filterable<?> filterable) {
    invalidate(filterable);
  }

  private void registerListenersFor(Collection<Class<? extends Event>> relevantEvents) {
    for (Class<? extends Event> event : relevantEvents) {
      if (listeningFor.contains(event)) continue;

      EventExecutor result;
      // Some special cases for a few events...
      if (PlayerCoarseMoveEvent.class.isAssignableFrom(event))
        result = (l, e) -> this.onPlayerMove((PlayerCoarseMoveEvent) e);
      else if (MatchPlayerDeathEvent.class.isAssignableFrom(event))
        result = (l, e) -> this.onPlayerDeath((MatchPlayerDeathEvent) e);
      else if (PlayerPartyChangeEvent.class.isAssignableFrom(event))
        result = (l, e) -> this.onPartyChange((PlayerPartyChangeEvent) e);
      else if (FlagStateChangeEvent.class.isAssignableFrom(event))
        result = (l, e) -> this.onFlagStateChange((FlagStateChangeEvent) e);

      // The rest of the events
      else {
        final MethodHandle handle;
        try {
          handle = MethodHandleUtils.getHandle(event);
        } catch (Exception e) {
          match
              .getLogger()
              .severe("Unable to get MethodHandle extracting Filterable or Player for " + event);
          e.printStackTrace();
          continue;
        }
        result =
            (l, e) -> {
              try {
                final Object o = handle.invoke(e);
                if (o instanceof Filterable) this.invalidate((Filterable<?>) o);
                else if (o instanceof Player) this.invalidate(this.match.getPlayer((Player) o));
                else
                  throw new IllegalStateException(
                      "A cached MethodHandle returned a non-expected type. Was: " + o.getClass());
              } catch (Throwable t) {
                match
                    .getLogger()
                    .severe("Unable to invoke cached MethodHandle extracting Filterable for " + e);
                t.printStackTrace();
              }
            };
      }

      listeningFor.add(event);

      PGM.get()
          .getServer()
          .getPluginManager()
          .registerEvent(event, this.dummyListener, EventPriority.MONITOR, result, PGM.get());
    }
  }

  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    // On movement events, check the player immediately instead of invalidating them.
    // We can't wait until the end of the tick because the player could move several
    // more times by then (i.e. if we received multiple packets from them in the same
    // tick) which would make region checks highly unreliable.
    MatchPlayer player = match.getPlayer(event.getPlayer());

    if (player != null) {
      this.invalidate(player);
      PGM.get().getServer().postToMainThread(PGM.get(), true, this::tick);
    }
  }

  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    invalidate(event.getVictim());
    ParticipantState killer = event.getKiller();

    if (killer != null && killer.getPlayer().isPresent()) {
      invalidate(killer.getPlayer().get());
    }
  }

  public void onPartyChange(PlayerPartyChangeEvent event) throws EventException {
    if (event.getNewParty() != null) {
      invalidate(event.getPlayer());
    } else {
      // Because all dynamic player filters are effectively wrapped in "___ and online",
      // force all filters false that are not already false before the player leaves.
      // Listeners don't need to do any cleanup as long as they don't hold on to
      // players that don't match the filter.
      this.listeners
          .columnMap()
          .forEach(
              (scope, column) -> {
                if (scope.isInstance(event.getPlayer())) {
                  // For each filter in this scope
                  column.forEach(
                      (filter, filterListeners) -> {
                        // If player joined very recently, they may not have a cached response yet
                        final Boolean response = this.lastResponses.get(filter, event.getPlayer());
                        if (response != null && response) {
                          filterListeners.fall.forEach(
                              listener ->
                                  dispatch(
                                      (FilterListener<? super MatchPlayer>) listener,
                                      filter,
                                      event.getPlayer(),
                                      false));
                        }
                      });
                }
              });

      event.yield();

      // Wait until after the event to remove them, in case they get invalidated during the event.
      dirtySet.remove(event.getPlayer());
      this.lastResponses.columnKeySet().remove(event.getPlayer());
    }
  }

  public void onFlagStateChange(FlagStateChangeEvent event) {
    this.invalidate(match);
  }

  /**
   * Gets the active {@link ReactorFactory.Reactor} created by the given factory for this match.
   *
   * @param factory a factory which was used to create a reactor for this match
   * @return an active reactor
   * @throws NullPointerException if no active reactor is found for the given factory
   */
  @SuppressWarnings("unchecked")
  public <T extends ReactorFactory.Reactor> @NotNull T getReactor(ReactorFactory<T> factory) {
    return (T) Objects.requireNonNull(this.activeReactors.get(factory), "reactor");
  }

  private static class DummyListener implements Listener {}
}
