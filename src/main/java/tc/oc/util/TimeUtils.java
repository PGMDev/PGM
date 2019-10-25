package tc.oc.util;

import java.util.Date;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.Instant;

public class TimeUtils {
  public static final Duration INFINITE_DURATION = Duration.millis(Long.MAX_VALUE);
  public static final Instant INF_FUTURE = new Instant(Long.MAX_VALUE);
  public static final Instant INF_PAST = new Instant(Long.MIN_VALUE);

  public static boolean isInfinite(Duration duration) {
    return duration.getMillis() == Long.MAX_VALUE;
  }

  public static boolean isInfFuture(Instant instant) {
    return instant.getMillis() == Long.MAX_VALUE;
  }

  public static boolean isInfPast(Instant instant) {
    return instant.getMillis() == Long.MIN_VALUE;
  }

  public static boolean isInfFuture(Date date) {
    return date.getYear() > 8000; // Hacky, but needs to match Ruby's Time::INF_FUTURE
  }

  public static boolean isInfPast(Date date) {
    return date.getYear() < -10000;
  }

  public static Instant toInstant(Date date) {
    if (isInfFuture(date)) {
      return INF_FUTURE;
    } else if (isInfPast(date)) {
      return INF_PAST;
    } else {
      return new Instant(date);
    }
  }

  public static long daysRoundingUp(Duration duration) {
    return (duration.getMillis() + DateTimeConstants.MILLIS_PER_DAY - 1)
        / DateTimeConstants.MILLIS_PER_DAY;
  }

  public static long toTicks(Duration duration) {
    return duration.getMillis() / 50;
  }

  public static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  public static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
  }
}
