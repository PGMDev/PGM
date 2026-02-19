package tc.oc.pgm.action.actions;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.filters.Filterable;

public class WaitAction<B extends Filterable<?>> extends AbstractAction<B> {
  private final Duration duration;
  private final Action<? super B> action;

  public WaitAction(Class<B> scope, Duration duration, Action<? super B> action) {
    super(scope);
    this.duration = duration;
    this.action = action;
  }

  @Override
  public void trigger(B t) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    scheduler.schedule(
        () -> {
          action.trigger(t);
        },
        duration.toMillis(),
        TimeUnit.MILLISECONDS);

    scheduler.shutdown();
  }
}
