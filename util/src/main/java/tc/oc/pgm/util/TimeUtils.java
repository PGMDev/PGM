package tc.oc.pgm.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {
  private TimeUtils() {}

  public static final long TICK = 50;
  public static final long MAX_TICK = Integer.MAX_VALUE;
  public static final Duration INFINITE_DURATION = ChronoUnit.FOREVER.getDuration();

  public static boolean isInfinite(Duration duration) {
    return duration.compareTo(INFINITE_DURATION) >= 0;
  }

  public static long toTicks(Duration duration) {
    try {
      return duration.toMillis() / TICK;
    } catch (ArithmeticException e) {
      return MAX_TICK;
    }
  }

  public static long toTicks(long amount, TimeUnit unit) {
    try {
      return unit.toMillis(amount) / TICK;
    } catch (ArithmeticException e) {
      return MAX_TICK;
    }
  }

  public static Duration fromTicks(long ticks) {
    return Duration.ofMillis(ticks * TICK);
  }

  public static boolean isShorterThan(Duration a, Duration b) {
    return a.compareTo(b) < 0;
  }

  public static boolean isLongerThan(Duration a, Duration b) {
    return a.compareTo(b) > 0;
  }

  public static Duration max(Duration a, Duration b) {
    return a.compareTo(b) > 0 ? a : b;
  }

  public static Duration min(Duration a, Duration b) {
    return a.compareTo(b) < 0 ? a : b;
  }
}
