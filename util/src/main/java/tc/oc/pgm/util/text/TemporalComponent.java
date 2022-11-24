package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public final class TemporalComponent {
  private TemporalComponent() {}

  private static final long SECOND = 1;
  private static final long MINUTE = SECOND * 60;
  private static final long HOUR = MINUTE * 60;
  private static final long DAY = HOUR * 24;
  private static final long WEEK = DAY * 7;
  private static final long MONTH = DAY * 30;
  private static final long YEAR = DAY * 365;
  private static final long A_LONG_TIME = 10 * YEAR;

  private static final String CLOCK_FORMAT = "%02d:%02d"; // MM:SS
  private static final String CLOCK_FORMAT_LONG = "%d:" + CLOCK_FORMAT; // HH:MM:SS

  /**
   * Creates a {@link Component} that represents a temporal duration.
   *
   * @param duration a duration
   * @param color a unit color
   * @return a component builder
   */
  public static TranslatableComponent duration(
      final @Nullable Duration duration, final @Nullable TextColor color) {
    if (duration == null) {
      return duration(A_LONG_TIME, color);
    }
    return duration(Math.abs(duration.getSeconds()), color);
  }

  /**
   * Creates a {@link Component} that represents a temporal duration.
   *
   * @param duration a duration
   * @return a component builder
   */
  public static TranslatableComponent duration(final @Nullable Duration duration) {
    return duration(duration, null);
  }

  /**
   * Creates a {@link Component} that represents a temporal duration.
   *
   * @param seconds a number of seconds
   * @param color a unit color
   * @return a component builder
   */
  public static TranslatableComponent duration(
      final long seconds, final @Nullable TextColor color) {
    final String key;
    long quantity = 1;

    if (seconds < SECOND) {
      key = "misc.now";
    } else if (seconds == SECOND) {
      key = "misc.second";
    } else if (seconds < MINUTE) {
      key = "misc.seconds";
      quantity = seconds;
    } else if (seconds < HOUR) {
      quantity = seconds / MINUTE;
      key = quantity == 1 ? "misc.minute" : "misc.minutes";
    } else if (seconds < DAY) {
      quantity = seconds / HOUR;
      key = quantity == 1 ? "misc.hour" : "misc.hours";
    } else if (seconds < WEEK) {
      quantity = seconds / DAY;
      key = quantity == 1 ? "misc.day" : "misc.days";
    } else if (seconds < MONTH) {
      quantity = seconds / WEEK;
      key = quantity == 1 ? "misc.week" : "misc.weeks";
    } else if (seconds < YEAR) {
      quantity = seconds / MONTH;
      key = quantity == 1 ? "misc.month" : "misc.months";
    } else if (seconds < A_LONG_TIME) {
      quantity = seconds / YEAR;
      key = quantity == 1 ? "misc.year" : "misc.years";
    } else {
      key = "misc.eon";
    }

    return translatable(key, text(quantity, color));
  }

  /**
   * Creates a {@link Component} that represents a ticking duration.
   *
   * @param seconds a number of seconds
   * @return a component builder
   */
  public static TextComponent ticker(final long seconds) {
    if (seconds >= MINUTE) {
      return text((seconds % HOUR) / MINUTE + "m");
    } else if (seconds >= SECOND) {
      return text(seconds + "s");
    } else {
      return text("<1s");
    }
  }

  /**
   * Creates a {@link Component} that represents a ticking duration.
   *
   * @param duration a duration
   * @return a component builder
   */
  public static TextComponent ticker(final Duration duration) {
    return ticker(duration.getSeconds());
  }

  /**
   * Creates a {@link Component} that represents a clock.
   *
   * @param seconds a number of seconds
   * @return a component builder
   */
  public static TextComponent clock(final long seconds) {
    if (seconds >= HOUR) {
      return text(
          String.format(
              CLOCK_FORMAT_LONG, seconds / HOUR, (seconds % HOUR) / MINUTE, (seconds % MINUTE)));
    } else {
      return text(String.format(CLOCK_FORMAT, (seconds % HOUR) / MINUTE, (seconds % MINUTE)));
    }
  }

  /**
   * Creates a {@link Component} that represents a clock.
   *
   * @param duration a duration
   * @return a component builder
   */
  public static TextComponent clock(final Duration duration) {
    if (duration.isNegative()) {
      return clock(0);
    }
    return clock(duration.getSeconds());
  }

  /**
   * Creates a {@link Component} that represents a duration in seconds.
   *
   * @param seconds a number of seconds
   * @param color a unit color
   * @return a component builder
   */
  public static TranslatableComponent seconds(final long seconds, final @Nullable TextColor color) {
    return translatable((seconds == 1) ? "misc.second" : "misc.seconds", text(seconds, color));
  }

  // TODO: Change these signature after the Community refactor

  @Deprecated
  public static Component briefNaturalApproximate(final Duration duration) {
    return duration(duration);
  }

  @Deprecated
  public static Component briefNaturalApproximate(final Instant begin, final Instant end) {
    return briefNaturalApproximate(Duration.between(begin, end));
  }

  @Deprecated
  public static Component relativePastApproximate(final Instant then) {
    return translatable(
        "misc.timeAgo", briefNaturalApproximate(Duration.between(then, Instant.now())));
  }
}
