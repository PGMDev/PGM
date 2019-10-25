package tc.oc.util.components;

import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;

public class PeriodFormats {
  public static final PeriodFormatter SHORTHAND =
      new PeriodFormatterBuilder()
          .appendYears()
          .appendSuffix("y")
          .appendMonths()
          .appendSuffix("mo")
          .appendDays()
          .appendSuffix("d")
          .appendHours()
          .appendSuffix("h")
          .appendMinutes()
          .appendSuffix("m")
          .appendSecondsWithOptionalMillis()
          .appendSuffix("s")
          .appendSecondsWithOptionalMillis() // numbers with no units assumed to be seconds
          .toFormatter();

  public static final PeriodFormatter COLONS =
      new PeriodFormatterBuilder()
          .appendHours()
          .appendSeparatorIfFieldsBefore(":")
          .minimumPrintedDigits(2)
          .printZeroAlways()
          .appendMinutes()
          .appendSeparator(":")
          .minimumPrintedDigits(2)
          .appendSeconds()
          .toFormatter();

  public static final PeriodFormatter COLONS_PRECISE =
      new PeriodFormatterBuilder()
          .appendHours()
          .appendSeparatorIfFieldsBefore(":")
          .minimumPrintedDigits(2)
          .printZeroAlways()
          .appendMinutes()
          .appendSeparator(":")
          .minimumPrintedDigits(2)
          .appendSecondsWithMillis()
          .toFormatter();

  public static final PeriodFormatter COUNTDOWN =
      new PeriodFormatterBuilder()
          .appendHours()
          .appendSuffix(" hr ")
          .appendMinutes()
          .appendSuffix(" min ")
          .appendSecondsWithOptionalMillis()
          .appendSuffix(" sec")
          .toFormatter();

  // The time units that we have translations for
  private static final DurationField[] UNITS =
      new DurationField[] {
        DurationFieldType.days().getField(ISOChronology.getInstance()),
        DurationFieldType.hours().getField(ISOChronology.getInstance()),
        DurationFieldType.minutes().getField(ISOChronology.getInstance()),
        DurationFieldType.seconds().getField(ISOChronology.getInstance()),
        DurationFieldType.millis().getField(ISOChronology.getInstance())
      };

  /** Return the key for the localized description of the given time interval */
  public static String periodKey(DurationFieldType unit, long quantity) {
    if (unit == DurationFieldType.days()) {
      return quantity == 1 ? "time.interval.day" : "time.interval.days";
    } else if (unit == DurationFieldType.hours()) {
      return quantity == 1 ? "time.interval.hour" : "time.interval.hours";
    } else if (unit == DurationFieldType.minutes()) {
      return quantity == 1 ? "time.interval.minute" : "time.interval.minutes";
    } else if (unit == DurationFieldType.seconds()) {
      return quantity == 1 ? "time.interval.second" : "time.interval.seconds";
    } else if (unit == DurationFieldType.millis()) {
      return quantity == 1 ? "time.interval.millisecond" : "time.interval.milliseconds";
    } else {
      throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
  }

  /**
   * Return the key for the localized description of the given time period, which must contain
   * exactly one field.
   */
  public static String periodKey(Period period) {
    if (period.size() != 1) {
      throw new IllegalArgumentException("Periods with multiple fields are not supported");
    }
    DurationFieldType unit = period.getFieldType(0);
    return periodKey(unit, period.get(unit));
  }

  /** Return a localized description of the given time interval. */
  public static Component formatPeriod(DurationFieldType unit, long quantity) {
    return new PersonalizedTranslatable(
        periodKey(unit, quantity), new PersonalizedText(String.valueOf(quantity)));
  }

  /**
   * Return a localized description of the given time period, which must contain exactly one field.
   */
  public static Component formatPeriod(Period period) {
    return new PersonalizedTranslatable(
        periodKey(period), new PersonalizedText(String.valueOf(period.getValue(0))));
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that can
   * represent the interval precisely. This is useful when the interval is expected to be a "round"
   * value. The interval must be non-zero.
   */
  public static Component briefNaturalPrecise(Duration duration) {
    if (duration.getMillis() == 0) {
      throw new IllegalArgumentException("Duration cannot be zero");
    }

    long ms = duration.getMillis();
    for (DurationField unit : UNITS) {
      if (ms % unit.getUnitMillis() == 0) {
        return formatPeriod(unit.getType(), unit.getValue(duration.getMillis()));
      }
    }
    throw new IllegalStateException();
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that fits
   * into the interval at least the given number of times. The interval must be non-zero.
   */
  public static Component briefNaturalApproximate(Duration duration, long minQuantity) {
    if (duration.getMillis() == 0) {
      throw new IllegalArgumentException("Duration cannot be zero");
    }

    for (DurationField unit : UNITS) {
      long quantity = unit.getValue(duration.getMillis());
      if (quantity >= minQuantity) {
        return formatPeriod(unit.getType(), quantity);
      }
    }
    throw new IllegalStateException();
  }

  public static Component briefNaturalApproximate(Instant begin, Instant end, long minQuantity) {
    return briefNaturalApproximate(new Duration(begin, end), minQuantity);
  }

  /**
   * A (localized) interval phrase like "X seconds" or "X minutes". Uses the largest unit that fits
   * into the interval at least twice. The interval must be non-zero.
   */
  public static Component briefNaturalApproximate(Duration duration) {
    return briefNaturalApproximate(duration, 2);
  }

  public static Component briefNaturalApproximate(Instant begin, Instant end) {
    return briefNaturalApproximate(begin, end, 2);
  }

  public static Component relativePastApproximate(Instant then) {
    return new PersonalizedTranslatable(
        "time.ago", briefNaturalApproximate(new Duration(then, Instant.now())));
  }
}
