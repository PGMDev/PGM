package tc.oc.pgm.countdowns;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.scheduler.BukkitRunnable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.util.ClassLogger;
import tc.oc.util.TimeUtils;

public class CountdownRunner extends BukkitRunnable {

  public static final Duration MIN_REPEAT_INTERVAL = Duration.ofMillis(50);

  protected final Match match;
  protected final Logger logger;
  protected final Countdown countdown;

  private int count;
  private @Nullable Duration interval;
  private @Nullable Instant start;
  private @Nullable Instant end;

  // The remaining seconds that will be passed to onTick for the next cycle
  private long secondsRemaining;

  private Future<?> task = null;

  public CountdownRunner(@Nonnull Match match, Logger parentLogger, @Nonnull Countdown countdown) {
    Preconditions.checkNotNull(match, "match");
    Preconditions.checkNotNull(countdown, "countdown");

    this.match = match;
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
    logger.fine("STARTING countdown " + countdown + " for duration " + remaining);

    if (interval != null && TimeUtils.isShorterThan(interval, MIN_REPEAT_INTERVAL) && count > 1) {
      throw new IllegalArgumentException(
          "Repeat interval must be at least " + MIN_REPEAT_INTERVAL.toMillis() + " milliseconds");
    }

    if (this.task == null && count > 0) {
      this.count = count;
      this.interval = interval;
      this.start = match.getTick().instant;
      this.end = this.start.plus(remaining);
      this.secondsRemaining = remaining.getSeconds();
      this.countdown.onStart(remaining, this.getTotalTime());

      this.task = match.getExecutor(MatchScope.LOADED).submit(this);
    }

    return this;
  }

  public void cancel() {
    if (this.isRunning()) {
      logger.fine("Cancelling countdown " + countdown);

      this.stop();
      Duration remaining = Duration.between(match.getTick().instant, this.end);
      this.countdown.onCancel(
          TimeUtils.isShorterThan(remaining, Duration.ZERO) ? Duration.ZERO : remaining,
          this.getTotalTime());
    }
  }

  protected void stop() {
    if (this.task != null) {
      this.task.cancel(true);
      this.task = null;
    }
  }

  public @Nullable Instant getStart() {
    return this.start;
  }

  public @Nullable Instant getEnd() {
    return this.end;
  }

  public Duration getTotalTime() {
    return Duration.between(this.start, this.end);
  }

  public long getSecondsRemaining() {
    return this.secondsRemaining;
  }

  @Override
  public void run() {
    this.task = null;
    if (this.end == null || this.secondsRemaining < 0) return;

    // Get the total ticks remaining in the countdown
    long ticksRemaining = TimeUtils.toTicks(Duration.between(match.getTick().instant, this.end));

    // Handle any cycles since the last one
    for (;
        this.secondsRemaining >= 0 && this.secondsRemaining * 20 >= ticksRemaining;
        this.secondsRemaining--) {
      this.countdown.onTick(Duration.ofSeconds(this.secondsRemaining), this.getTotalTime());
    }

    if (this.secondsRemaining >= 0) {
      // If there are cycles left, schedule the next run
      this.task = match.getExecutor(MatchScope.LOADED).schedule(this, 1, TimeUnit.SECONDS);
    } else {
      // Otherwise, finish the countdown
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
