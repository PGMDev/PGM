package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public final class TemporalComponent {
  private TemporalComponent() {}

  private static final long SECOND = 1;
  private static final long MINUTE = SECOND * 60;
  private static final long HOUR = MINUTE * 60;
  private static final long DAY = HOUR * 24;
  private static final long WEEK = DAY * 7;
  private static final long MONTH = DAY * 30;
  private static final long YEAR = DAY * 365;
  private static final long A_LONG_TIME = 3 * YEAR;

  /**
   * Creates a {@link Component} that represents a temporal duration.
   *
   * @param duration a duration
   * @return a component builder
   */
  public static TranslatableComponent.Builder duration(final @Nullable Duration duration) {
    if (duration == null) {
      return duration(A_LONG_TIME);
    }
    return duration(Math.abs(duration.getSeconds()));
  }

  /**
   * Creates a {@link Component} that represents a temporal duration.
   *
   * @param seconds a number of seconds
   * @return a component builder
   */
  public static TranslatableComponent.Builder duration(final long seconds) {
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

    return translatable().key(key).args(text(quantity));
  }

  // TODO: Change these signature after the Community refactor

  @Deprecated
  public static Component briefNaturalApproximate(final Duration duration) {
    return duration(duration).build();
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
