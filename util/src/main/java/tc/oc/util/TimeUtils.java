package tc.oc.util;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
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

  public static Duration parseDuration(String text) {
    if (text.equalsIgnoreCase("oo")) {
      return INFINITE_DURATION;
    }

    text = text.toLowerCase();

    int index = text.indexOf("d"); // days
    if (index > 0) {
      text = "p" + text.substring(0, ++index) + "t" + text.substring(++index);
    } else {
      text = "pt" + text;
    }

    try {
      return Duration.parse(text);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Unable to parse '" + text + "' into a duration", e);
    }
  }

  public static boolean isInfinite(Duration duration) {
    return duration.compareTo(INFINITE_DURATION) >= 0;
  }

  public static long toTicks(Duration duration) {
    try {
      return (duration.toMillis() + 49) / 50;
    } catch (ArithmeticException e) {
      return Integer.MAX_VALUE;
    }
  }

  public static boolean isShorterThan(Duration a, Duration b) {
    return a.compareTo(b) < 0;
  }

  public static boolean isLongerThan(Duration a, Duration b) {
    return a.compareTo(b) > 0;
  }
}
