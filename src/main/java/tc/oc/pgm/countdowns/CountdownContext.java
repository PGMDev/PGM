package tc.oc.pgm.countdowns;

import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.util.ClassLogger;

public class CountdownContext {
  protected final Match match;
  protected final Logger logger;
  protected final Map<Countdown, CountdownRunner> runners = new HashMap<>();

  public CountdownContext(Match match, Logger parentLogger) {
    this.match = match;
    this.logger = ClassLogger.get(parentLogger, getClass());
  }

  public void start(Countdown countdown, int seconds) {
    this.start(countdown, Duration.standardSeconds(seconds));
  }

  public void start(Countdown countdown, Duration duration) {
    this.start(countdown, duration, null);
  }

  public void start(Countdown countdown, Duration duration, @Nullable Duration interval) {
    this.start(countdown, duration, interval, 1);
  }

  /**
   * Start running the given {@link Countdown} in this context. If the given Countdown is already
   * running, it will be cancelled and restarted with the given duration.
   */
  public void start(
      Countdown countdown, Duration duration, @Nullable Duration interval, int count) {
    CountdownRunner runner = this.runners.get(countdown);
    if (runner != null) runner.cancel();
    this.runners.put(
        countdown,
        new CountdownRunner(match, this.logger, countdown).start(duration, interval, count));
  }

  public void cancel(Countdown countdown) {
    CountdownRunner runner = this.runners.remove(countdown);
    if (runner != null) {
      runner.cancel();
    }
  }

  public Set<Countdown> getAll() {
    return ImmutableSet.copyOf(this.runners.keySet());
  }

  @SuppressWarnings("unchecked")
  public <T extends Countdown> Set<T> getAll(Class<T> countdownClass) {
    Set<T> result = new HashSet<>();
    for (Countdown countdown : this.runners.keySet()) {
      if (countdownClass.isInstance(countdown)) {
        result.add((T) countdown);
      }
    }
    return result;
  }

  public Duration getTimeLeft(Countdown countdown) {
    CountdownRunner runner = this.runners.get(countdown);
    return runner == null ? null : Duration.standardSeconds(runner.getSecondsRemaining());
  }

  public boolean isRunning(Countdown countdown) {
    CountdownRunner runner = this.runners.get(countdown);
    return runner != null && runner.getSecondsRemaining() > 0;
  }

  public boolean isFinished(Countdown countdown) {
    CountdownRunner runner = this.runners.get(countdown);
    return runner != null && runner.getSecondsRemaining() <= 0;
  }

  public void cancelAll() {
    logger.fine("Cancelling all countdowns");

    for (Iterator<CountdownRunner> it = this.runners.values().iterator(); it.hasNext(); ) {
      CountdownRunner runner = it.next();
      it.remove(); // Remove before callback to prevent infinite recursion
      try {
        runner.cancel();
      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Exception cancelling countdown " + runner.getCountdown(), e);
      }
    }
  }

  /**
   * Cancel all countdowns of the given type
   *
   * @return true if any countdowns were cancelled
   */
  public boolean cancelAll(Class<? extends Countdown> countdownClass) {
    logger.fine("Cancelling all " + countdownClass.getSimpleName() + " countdowns");

    boolean cancelled = false;
    for (Iterator<Countdown> it = this.runners.keySet().iterator(); it.hasNext(); ) {
      Countdown countdown = it.next();
      if (countdownClass.isInstance(countdown)) {
        CountdownRunner runner = this.runners.get(countdown);
        it.remove(); // Remove before callback to prevent infinite recursion
        runner.cancel();
        cancelled = true;
      }
    }
    return cancelled;
  }

  /**
   * Cancel all countdowns that are not of the given type
   *
   * @return true if any countdowns were cancelled
   */
  public boolean cancelOthers(Class<? extends Countdown> countdownClass) {
    logger.fine("Cancelling all countdowns except " + countdownClass.getSimpleName());

    boolean cancelled = false;
    for (Iterator<Countdown> it = this.runners.keySet().iterator(); it.hasNext(); ) {
      Countdown countdown = it.next();
      if (!countdownClass.isInstance(countdown)) {
        CountdownRunner runner = this.runners.get(countdown);
        it.remove(); // Remove before callback to prevent infinite recursion
        runner.cancel();
        cancelled = true;
      }
    }
    return cancelled;
  }
}
