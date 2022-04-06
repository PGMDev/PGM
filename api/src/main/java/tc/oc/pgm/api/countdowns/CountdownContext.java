package tc.oc.pgm.api.countdowns;

import java.time.Duration;
import java.util.Set;
import javax.annotation.Nullable;

public interface CountdownContext {
  void start(Countdown countdown, int seconds);

  void start(Countdown countdown, Duration duration);

  void start(Countdown countdown, Duration duration, @Nullable Duration interval);

  void start(Countdown countdown, Duration duration, @Nullable Duration interval, int count);

  void cancel(Countdown countdown);

  Set<Countdown> getAll();

  @SuppressWarnings("unchecked")
  <T extends Countdown> Set<T> getAll(Class<T> countdownClass);

  Duration getTimeLeft(Countdown countdown);

  boolean isRunning(Countdown countdown);

  boolean isFinished(Countdown countdown);

  void cancelAll();

  boolean cancelAll(Class<? extends Countdown> countdownClass);

  boolean cancelOthers(Class<? extends Countdown> countdownClass);

  boolean cancelOthers(Countdown except);
}
