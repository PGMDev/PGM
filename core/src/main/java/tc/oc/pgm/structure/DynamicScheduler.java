package tc.oc.pgm.structure;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;

/**
 * We need this to make sure that dynamics are placed and cleared in definition order, and that
 * clears happen before placements.
 */
public class DynamicScheduler {

  private final Match match;
  private final Queue<Dynamic> clearQueue;
  private final Queue<Dynamic> placeQueue;

  DynamicScheduler(Match match, Comparator<Dynamic> order) {
    this.match = match;

    this.clearQueue = new PriorityQueue<>(order);
    this.placeQueue = new PriorityQueue<>(order);
  }

  void queuePlace(Dynamic dynamic) {
    clearQueue.remove(dynamic);
    placeQueue.add(dynamic);
    schedule();
  }

  void queueClear(Dynamic dynamic) {
    placeQueue.remove(dynamic);
    clearQueue.add(dynamic);
    schedule();
  }

  private void schedule() {
    match.getExecutor(MatchScope.LOADED).submit(this::process);
    // match.getScheduler(MatchScope.LOADED)
    //  .debounceTask(this::process);
  }

  public void process() {
    while (!clearQueue.isEmpty()) {
      final Dynamic dynamic = clearQueue.poll();
      dynamic.clear();
    }

    while (!placeQueue.isEmpty()) {
      Dynamic dynamic = placeQueue.poll();
      dynamic.place();
    }
  }
}
