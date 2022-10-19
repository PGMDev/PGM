package tc.oc.pgm.countdowns;

import java.time.Duration;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

public class SingleCountdownContext extends CountdownContext {

  public SingleCountdownContext(Match match, Logger parentLogger) {
    super(match, parentLogger);
  }

  @Override
  public void start(
      Countdown countdown, Duration duration, @Nullable Duration interval, int count) {
    this.cancelOthers(countdown);
    super.start(countdown, duration, interval, count);
  }

  public @Nullable Countdown getCountdown() {
    return this.runners.isEmpty() ? null : this.runners.keySet().iterator().next();
  }

  public @Nullable <T extends Countdown> T getCountdown(Class<T> type) {
    if (this.runners.isEmpty()) return null;
    Countdown countdown = this.runners.keySet().iterator().next();
    return type.isInstance(countdown) ? (T) countdown : null;
  }
}
