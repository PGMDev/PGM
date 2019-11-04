package tc.oc.pgm.filters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.match.MatchModule;

public class FilterMatchModule extends MatchModule implements Listener {

  private final SetMultimap<Filter, FilterListener> listeners = HashMultimap.create();
  private final Map<Filter, Filter.QueryResponse> responses = new HashMap<>();

  private final PriorityQueue<TimeFilter> timeFilterQueue = new PriorityQueue<>();

  public FilterMatchModule(Match match) {
    super(match);
  }

  /**
   * Listen for changes in the response by the given filter to a global {@link
   * tc.oc.pgm.filters.query.IMatchQuery}. See {@link FilterListener} for more details. Currently,
   * only global queries are supported, and only {@link TimeFilter}s, {@link GoalFilter}s, {@link
   * FlagStateFilter}s, and {@link CarryingFlagFilter}s are guaranteed to notify for all changes.
   */
  public void listen(Filter filter, FilterListener listener) {
    listeners.put(filter, listener);

    Filter.QueryResponse response = responses.get(filter);
    if (response == null) {
      response = filter.query(getMatch().getQuery());
      responses.put(filter, response);
    }

    listener.filterQueryChanged(filter, getMatch().getQuery(), null, response);
  }

  @Override
  public void load() {
    super.load();
    for (TimeFilter filter : getMatch().getMapContext().features().getAll(TimeFilter.class)) {
      timeFilterQueue.add(filter);
    }
  }

  @Override
  public void enable() {
    super.enable();
    checkTimeFilters();
  }

  void check(Filter filter) {
    Filter.QueryResponse newResponse = filter.query(getMatch().getQuery());
    Filter.QueryResponse oldResponse = responses.put(filter, newResponse);

    if (oldResponse != newResponse) {
      for (FilterListener listener : listeners.get(filter)) {
        listener.filterQueryChanged(filter, getMatch().getQuery(), oldResponse, newResponse);
      }
    }
  }

  void checkAll() {
    for (Filter filter : listeners.keySet()) {
      check(filter);
    }
  }

  void checkTimeFilters() {
    if (!getMatch().isRunning()) return;

    Duration now = getMatch().getDuration();
    boolean removed = false;
    TimeFilter next;
    for (; ; ) {
      next = timeFilterQueue.peek();
      if (next == null || next.getTime().isLongerThan(now)) break;
      timeFilterQueue.remove();
      removed = true;
    }

    if (removed) checkAll();

    if (next != null) {
      getMatch()
          .getScheduler(MatchScope.RUNNING)
          .runTaskLater(
              next.getTime().minus(now),
              new Runnable() {
                @Override
                public void run() {
                  checkTimeFilters();
                }
              });
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onGoalComplete(GoalCompleteEvent event) {
    checkAll();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagChange(FlagStateChangeEvent event) {
    checkAll();
  }
}
