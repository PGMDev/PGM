package tc.oc.pgm.countdowns;

import com.google.common.base.Preconditions;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.time.TickClock;
import tc.oc.util.logging.ClassLogger;

public class CountdownRunner extends BukkitRunnable {

  public static final Duration MIN_REPEAT_INTERVAL = Duration.millis(50);

  protected final Plugin plugin;
  protected final TickClock clock;
  protected final Logger logger;
  protected final Countdown countdown;

  private int count;
  private @Nullable Duration interval;
  private @Nullable Instant start;
  private @Nullable Instant end;

  // The remaining seconds that will be passed to onTick for the next cycle
  private long secondsRemaining;

  private BukkitTask task = null;

  public CountdownRunner(
      @Nonnull Plugin plugin, TickClock clock, Logger parentLogger, @Nonnull Countdown countdown) {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(countdown, "countdown");

    this.plugin = plugin;
    this.clock = clock;
    this.logger = ClassLogger.get(parentLogger, getClass());
    this.countdown = countdown;
  }

  public boolean isRunning() {
    return this.task != null;
  }

  public @Nonnull CountdownRunner start(Duration remaining) {
    return this.start(remaining, null);
  }

  public @Nonnull CountdownRunner start(Duration remaining, @Nullable Duration interval) {
    return this.start(remaining, interval, 1);
  }

  public @Nonnull CountdownRunner start(
      Duration remaining, @Nullable Duration interval, int count) {
    logger.fine("Starting countdown " + countdown + " for duration " + remaining);

    if (interval != null && interval.isShorterThan(MIN_REPEAT_INTERVAL) && count > 1) {
      throw new IllegalArgumentException(
          "Repeat interval must be at least " + MIN_REPEAT_INTERVAL.getMillis() + " milliseconds");
    }

    if (this.task == null && count > 0) {
      this.count = count;
      this.interval = interval;
      this.start = this.clock.now().instant;
      this.end = this.start.plus(remaining);
      this.secondsRemaining = remaining.getStandardSeconds();
      this.countdown.onStart(remaining, this.getTotalTime());

      this.task = this.getScheduler().runTask(this.plugin, this);
    }

    return this;
  }

  public void cancel() {
    if (this.isRunning()) {
      logger.fine("Cancelling countdown " + countdown);

      this.stop();
      Duration remaining = new Duration(this.clock.now().instant, this.end);
      this.countdown.onCancel(
          remaining.isShorterThan(Duration.ZERO) ? Duration.ZERO : remaining, this.getTotalTime());
    }
  }

  protected void stop() {
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }
  }

  protected BukkitScheduler getScheduler() {
    return this.plugin.getServer().getScheduler();
  }

  public @Nullable Instant getStart() {
    return this.start;
  }

  public @Nullable Instant getEnd() {
    return this.end;
  }

  public Duration getTotalTime() {
    return new Duration(this.start, this.end);
  }

  public long getSecondsRemaining() {
    return this.secondsRemaining;
  }

  @Override
  public void run() {
    this.task = null;
    if (this.end == null || this.secondsRemaining < 0) return;

    // Get the total ticks remaining in the countdown
    long ticksRemaining =
        Math.round(new Duration(this.clock.now().instant, this.end).getMillis() / 50d);

    // Handle any cycles since the last one
    for (;
        this.secondsRemaining >= 0 && this.secondsRemaining * 20 >= ticksRemaining;
        this.secondsRemaining--) {
      this.countdown.onTick(Duration.standardSeconds(this.secondsRemaining), this.getTotalTime());
    }

    if (this.secondsRemaining >= 0) {
      // If there are cycles left, schedule the next run
      long ticks = ticksRemaining - this.secondsRemaining * 20;
      this.task = this.getScheduler().runTaskLater(this.plugin, this, ticks < 1 ? 1 : ticks);
    } else {
      // Otherwise, end the countdown
      logger.fine("Ending countdown " + countdown);

      this.secondsRemaining = 0;
      this.countdown.onEnd(this.getTotalTime());

      if (this.interval != null) {
        this.start(
            this.interval,
            this.interval,
            this.count == Integer.MAX_VALUE ? this.count : this.count - 1);
      }
    }
  }

  public @Nonnull Countdown getCountdown() {
    return this.countdown;
  }
}
