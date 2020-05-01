package tc.oc.pgm.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {
  private TimeUtils() {}

  public static final long TICK = 50;
  public static final long MAX_TICK = Integer.MAX_VALUE;
  public static final Duration INFINITE_DURATION = ChronoUnit.FOREVER.getDuration();

  private static final String DURATION_MINUTES_FORMAT = "%02d:%02d";
  private static final String DURATION_HOURS_FORMAT = "%d:" + DURATION_MINUTES_FORMAT;

  public static String formatDuration(Duration duration) {
    long secs = duration.getSeconds();
    if (secs >= 3600) {
      return String.format(DURATION_HOURS_FORMAT, secs / 3600, (secs % 3600) / 60, (secs % 60));
    } else {
      return String.format(DURATION_MINUTES_FORMAT, (secs % 3600) / 60, (secs % 60));
    }
  }

  public static String formatDurationShort(Duration duration) {
    long secs = duration.getSeconds();
    if (secs >= 60) {
      return (secs % 3600) / 60 + "m";
    } else if (secs >= 1) {
      return secs + "s";
    } else {
      return "<1s";
    }
  }

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

  public static boolean isShorterThan(Duration a, Duration b) {
    return a.compareTo(b) < 0;
  }

  public static boolean isLongerThan(Duration a, Duration b) {
    return a.compareTo(b) > 0;
  }
}
