package tc.oc.pgm.filters;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.util.MapUtils;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/**
 * Handler of dynamic filter magic &#x1F52E;.
 *
 * <p>A {@link Filter} can have the possibility of being dynamic or not, but the match module
 * decides whether to use a filter dynamically or not. In this sense the docs can be a little
 * misleading when thinking about dynamic filters programatically. The docs say "the [dynamic]
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
public class FilterMatchModule implements MatchModule, Listener, FilterDispatcher, Tickable {

  private final Match match;
  private final PriorityQueue<TimeFilter> timeFilterQueue = new PriorityQueue<>();

  public FilterMatchModule(Match match) {
    this.match = match;
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

  /**
   * Registers a filter listener for the given scope to be notified when the response of the
   * provided filter is equal to the provided response.
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param response The desired response
   * @param listener The listener that handles the response
   */
  private <F extends Filterable<?>> void register(
      Class<F> scope, Filter filter, boolean response, FilterListener<? super F> listener) {
    if (match.isLoaded()) {
      throw new IllegalStateException("Cannot register filter listener after match is loaded");
    }

    final ListenerSet listenerSet =
        listeners.row(filter).computeIfAbsent(scope, s -> new ListenerSet());

    (response ? listenerSet.rise : listenerSet.fall).add(listener);

    match
        .filterableDescendants(scope)
        .forEach(
            filterable -> {
              final boolean last = lastResponse(filter, filterable);
              if (last == response) {
                dispatch(listener, filter, filterable, last);
              }
            });
  }

  /**
   * {@inheritDoc}
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
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
   * {@inheritDoc}
   *
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  @Override
  public void onChange(
      Filter filter, tc.oc.pgm.filters.FilterListener<? super Filterable<?>> listener) {
    onChange((Class) Filterable.class, filter, listener);
  }

  /**
   * {@inheritDoc}
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
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
   * {@inheritDoc}
   *
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  @Override
  public void onRise(Filter filter, Consumer<? super Filterable<?>> listener) {
    onRise((Class) Filterable.class, filter, listener);
  }

  /**
   * {@inheritDoc}
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
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
   * {@inheritDoc}
   *
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  @Override
  public void onFall(Filter filter, Consumer<? super Filterable<?>> listener) {
    onFall((Class) Filterable.class, filter, listener);
  }

  /** Returns the last response a given filter gave to a given filterable */
  private boolean lastResponse(Filter filter, Filterable<?> filterable) {
    return MapUtils.computeIfAbsent(lastResponses.row(filter), filterable, filter::response);
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
    final Map<Filter, Boolean> afterCache = lastResponses.column(filterable);

    // For each scope that the given filterable applies to
    listeners
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
   * Checks the response for a query for all filters that fits a scope, if the response is different
   * than the last cached response and the filters cares about the change the responses are
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
    for (; ; ) {
      // Collect Filterables that are dirty, and have not already been checked in this tick
      final Set<Filterable<?>> checking = ImmutableSet.copyOf(Sets.difference(dirtySet, checked));
      if (checking.isEmpty()) break;

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
      filterable.filterableChildren().forEach(this::invalidate);
    }
  }

  /** TODO: optimize using the filter parameter */
  public void invalidate(Filter filter, Filterable<?> filterable) {
    invalidate(filterable);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerCoarseMoveEvent event) {
    // On movement events, check the player immediately instead of invalidating them.
    // We can't wait until the end of the tick because the player could move several
    // more times by then (i.e. if we received multiple packets from them in the same
    // tick) which would make region checks highly unreliable.
    MatchPlayer player = match.getPlayer(event.getPlayer());

    if (player != null) {
      invalidate(player);
      PGM.get().getServer().postToMainThread(PGM.get(), true, this::tick);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    invalidate(event.getVictim());
    ParticipantState killer = event.getKiller();

    if (killer != null && killer.getPlayer().isPresent()) {
      invalidate(killer.getPlayer().get());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyChange(PlayerPartyChangeEvent event) throws EventException {
    if (event.getNewParty() != null) {
      invalidate(event.getPlayer());
    } else {
      // Because all dynamic player filters are effectively wrapped in "___ and online",
      // force all filters false that are not already false before the player leaves.
      // Listeners don't need to do any cleanup as long as they don't hold on to
      // players that don't match the filter.
      listeners
          .columnMap()
          .forEach(
              (scope, column) -> {
                if (scope.isInstance(event.getPlayer())) {
                  // For each filter in this scope
                  column.forEach(
                      (filter, filterListeners) -> {
                        // If player joined very recently, they may not have a cached response yet
                        final Boolean response = lastResponses.get(filter, event.getPlayer());
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
      dirtySet.remove(event.getPlayer().getQuery());
      lastResponses.columnKeySet().remove(event.getPlayer().getQuery());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStateChange(MatchPhaseChangeEvent event) {
    invalidate(match);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalComplete(GoalCompleteEvent event) {
    invalidate(match);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagChange(FlagStateChangeEvent event) {
    invalidate(match);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onScoreChange(CompetitorScoreChangeEvent event) {
    invalidate(match);
  }

  // TODO: Invalidate match on RankingsChangeEvent if/when it exists
}
