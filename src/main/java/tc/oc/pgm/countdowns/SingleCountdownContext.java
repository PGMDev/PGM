package tc.oc.pgm.countdowns;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import tc.oc.pgm.time.TickClock;

public class SingleCountdownContext extends CountdownContext {

  public SingleCountdownContext(Plugin plugin, TickClock clock, Logger parentLogger) {
    super(plugin, clock, parentLogger);
  }

  @Override
  public void start(
      Countdown countdown, Duration duration, @Nullable Duration interval, int count) {
    this.cancelAll();
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
