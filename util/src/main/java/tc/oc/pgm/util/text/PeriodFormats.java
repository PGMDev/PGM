package tc.oc.pgm.util.text;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;

public class PeriodFormats {

  // The time units that we have translations for
  private static final TemporalUnit[] UNITS =
      new TemporalUnit[] {
        ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS, ChronoUnit.MILLIS
      };

  /** Return the key for the localized description of the given time interval */
  private static String periodKey(TemporalUnit unit, long quantity) {
    if (unit == ChronoUnit.DAYS) {
      return quantity == 1 ? "misc.day" : "misc.days";
    } else if (unit == ChronoUnit.HOURS) {
      return quantity == 1 ? "misc.hour" : "misc.hours";
    } else if (unit == ChronoUnit.MINUTES) {
      return quantity == 1 ? "misc.minute" : "misc.minutes";
    } else if (unit == ChronoUnit.SECONDS) {
      return quantity == 1 ? "misc.second" : "misc.seconds";
    } else if (unit == ChronoUnit.MILLIS) {
      return quantity == 1 ? "misc.millisecond" : "misc.milliseconds";
    } else {
      throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
  }

  private static long periodAmount(TemporalUnit unit, Duration duration) {
    if (unit == ChronoUnit.DAYS) {
      return duration.toDays();
    } else if (unit == ChronoUnit.HOURS) {
      return duration.toHours();
    } else if (unit == ChronoUnit.MINUTES) {
      return duration.toMinutes();
    } else if (unit == ChronoUnit.SECONDS) {
      return duration.getSeconds();
    } else if (unit == ChronoUnit.MILLIS) {
      return duration.toMillis();
    } else {
      throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
  }

  /**
   * Return the key for the localized description of the given time period, which must contain
   * exactly one field.
   */
  private static String periodKey(Period period) {
    List<TemporalUnit> units = period.getUnits();
    if (units.size() != 1) {
      throw new IllegalArgumentException("Periods with multiple units are not supported");
    }
    TemporalUnit unit = units.get(0);
    return periodKey(unit, period.get(unit));
  }

  /** Return a localized description of the given time interval. */
  public static Component formatPeriod(TemporalUnit unit, long quantity) {
    return TranslatableComponent.of(
        periodKey(unit, quantity), TextComponent.of(String.valueOf(quantity)));
  }

  /**
   * Return a localized description of the given time period, which must contain exactly one field.
   */
  public static Component formatPeriod(Period period) {
    return TranslatableComponent.of(
        periodKey(period), TextComponent.of(String.valueOf(period.get(period.getUnits().get(0)))));
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that can
   * represent the interval precisely. This is useful when the interval is expected to be a "round"
   * value. The interval must be non-zero.
   */
  public static Component briefNaturalPrecise(Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("Duration must be positive");
    }

    long ms = duration.toMillis();
    for (TemporalUnit unit : UNITS) {
      if (ms % unit.getDuration().toMillis() == 0) {
        return formatPeriod(unit, periodAmount(unit, duration));
      }
    }
    throw new IllegalStateException();
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that fits
   * into the interval at least the given number of times. The interval must be non-zero.
   */
  public static Component briefNaturalApproximate(
      Duration duration, long minQuantity, boolean avoidMillis) {
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("Duration must be positive");
    }

    for (TemporalUnit unit : UNITS) {
      long quantity = periodAmount(unit, duration);
      if (quantity >= minQuantity || avoidMillis && unit == ChronoUnit.SECONDS) {
        return formatPeriod(unit, quantity);
      }
    }
    throw new IllegalStateException();
  }

  public static Component briefNaturalApproximate(Instant begin, Instant end, long minQuantity) {
    return briefNaturalApproximate(Duration.between(begin, end), minQuantity, true);
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that fits
   * into the interval at least twice. The interval must be non-zero.
   */
  public static Component briefNaturalApproximate(Duration duration) {
    return briefNaturalApproximate(duration, 2, true);
  }

  public static Component briefNaturalApproximate(Instant begin, Instant end) {
    return briefNaturalApproximate(begin, end, 2);
  }

  public static Component relativePastApproximate(Instant then) {
    return TranslatableComponent.of(
        "misc.timeAgo", briefNaturalApproximate(Duration.between(then, Instant.now())));
  }
}
