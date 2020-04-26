package tc.oc.pgm.util.text;

import static tc.oc.pgm.util.text.TextException.invalidFormat;
import static tc.oc.pgm.util.text.TextException.outOfRange;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import org.bukkit.Chunk;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.Version;

/** A string parser that generates user-friendly error messages. */
public final class TextParser {
  private TextParser() {}

  private static final Pattern YES = Pattern.compile("^true|yes|on$", Pattern.CASE_INSENSITIVE);
  private static final Pattern NO = Pattern.compile("^false|no|off$", Pattern.CASE_INSENSITIVE);
  private static final Pattern INF = Pattern.compile("^((\\+|-)?oo)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern DOT = Pattern.compile("\\s*\\.\\s*");
  private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
  private static final Range<Integer> NONNEG = Range.atLeast(0);

  /**
   * Parses text into a boolean.
   *
   * <p>Accepts variants such as "on", "off", or "yes".
   *
   * @param text The text.
   * @return A boolean.
   * @throws TextException If the text is not a boolean.
   */
  public static boolean parseBoolean(String text) throws TextException {
    if (YES.matcher(text).matches()) return true;
    if (NO.matcher(text).matches()) return false;

    throw invalidFormat(text, boolean.class, null);
  }

  /**
   * Parses text into an integer.
   *
   * <p>Accepts infinity as "+oo" or "-oo".
   *
   * @param text The text.
   * @param range A range of acceptable integers.
   * @return An integer.
   * @throws TextException If the text is invalid or out of range.
   */
  public static int parseInteger(String text, Range<Integer> range) throws TextException {
    final int number;
    if (INF.matcher(text).matches()) {
      number = text.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    } else {
      try {
        number = Integer.parseInt(text, 10); // Base 10
      } catch (NumberFormatException e) {
        throw invalidFormat(text, int.class, e);
      }
    }

    if (!range.contains(number)) {
      throw outOfRange(text, range);
    }

    return number;
  }

  /**
   * Parses text into a float.
   *
   * <p>Accepts infinity as "+oo" or "-oo".
   *
   * @param text The text.
   * @param range A range of acceptable floats.
   * @return A float.
   * @throws TextException If the text is invalid or out of range.
   */
  public static float parseFloat(String text, Range<Float> range) throws TextException {
    final float number;
    if (INF.matcher(text).matches()) {
      number = text.startsWith("-") ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    } else {
      try {
        number = Float.parseFloat(text);
      } catch (NumberFormatException e) {
        throw invalidFormat(text, float.class, e);
      }
    }

    if (!range.contains(number)) {
      throw outOfRange(text, range);
    }

    return number;
  }

  /**
   * Parses text into a duration.
   *
   * <p>For backwards compatibility, also accepts a number of seconds.
   *
   * @param text The text.
   * @param range A range of acceptable durations.
   * @return A duration.
   * @throws TextException If the text is invalid or out of range.
   * @see Duration#parse(CharSequence)
   */
  public static Duration parseDuration(String text, Range<Duration> range) throws TextException {
    Duration duration;
    if (INF.matcher(text).matches()) {
      duration = TimeUtils.INFINITE_DURATION;
      if (text.startsWith("-")) {
        duration = duration.negated();
      }
    } else {
      try {
        // Java parses durations using ISO-8601 standard, which are not user friendly
        // We modify the text slightly to remove the requirement for the "p" or "t" prefixes
        final String format;
        int index = text.indexOf("d"); // days

        if (index > 0) {
          format = "p" + text.substring(0, ++index) + "t" + text.substring(index);
        } else {
          format = "pt" + text;
        }

        duration = Duration.parse(format);
      } catch (DateTimeParseException e1) {
        // Backwards compatibility for fractional number of seconds
        try {
          duration = Duration.of((long) (parseFloat(text, Range.all()) * 1000), ChronoUnit.MILLIS);
        } catch (TextException e2) {
          throw invalidFormat(text, Duration.class, e1);
        }
      }
    }

    if (!range.contains(duration)) {
      throw TextException.outOfRange(text, range);
    }

    return duration;
  }

  /**
   * Parses text into a 3D vector.
   *
   * @param text The text.
   * @param rangeXZ A range of acceptable X or Z values.
   * @param rangeY A range of acceptable Y values.
   * @return A 3D vector.
   * @throws TextException If the text is invalid or out of range.
   */
  public static Vector parseVector3d(String text, Range<Float> rangeXZ, Range<Float> rangeY)
      throws TextException {
    final boolean twod = rangeY == null; // If 2D, then y = 0
    final Class<?> type = twod ? Chunk.class : Vector.class;
    final String[] components = COMMA.split(text, 3);

    if (components.length != (twod ? 2 : 3)) {
      throw invalidFormat(text, type, null);
    }

    return new Vector(
        parseFloat(components[0], rangeXZ),
        twod ? 0 : parseFloat(components[1], rangeY),
        parseFloat(components[twod ? 1 : 2], rangeXZ));
  }

  /**
   * Parses text into a 2D vector.
   *
   * @param text The text.
   * @param range A range of acceptable X or Z values.
   * @return A 2D vector, the Y value is always 0.
   * @throws TextException If the text is invalid or out of range.
   */
  public static Vector parseVector2d(String text, Range<Float> range) throws TextException {
    return parseVector3d(text, range, null);
  }

  /**
   * Parses text into an "x.y.z" version.
   *
   * @param text The text.
   * @param range A range of acceptable versions.
   * @return A version.
   * @throws TextException If the text is invalid or out of range.
   */
  public static Version parseVersion(String text, Range<Version> range) throws TextException {
    final String[] components = DOT.split(text, 3);
    final int size = components.length;

    if (size < 1 || size > 3) {
      throw invalidFormat(text, Version.class, null);
    }

    final int major = parseInteger(components[0], NONNEG);
    final int minor = size < 2 ? 0 : parseInteger(components[1], NONNEG);
    final int patch = size < 3 ? 0 : parseInteger(components[2], NONNEG);

    final Version version = new Version(major, minor, patch);

    if (!range.contains(version)) {
      throw outOfRange(text, range);
    }

    return version;
  }

  /**
   * Parses text into an enum.
   *
   * @param text The text.
   * @param type The enum class.
   * @param range A range of acceptable enums.
   * @param fuzzyMatch Whether non-exact matches can be returned.
   * @param <E> The enum type.
   * @return An enum.
   * @throws TextException If the text is invalid or out of range.
   */
  public static <E extends Enum<E>> E parseEnum(
      String text, Class<E> type, Range<E> range, boolean fuzzyMatch) throws TextException {
    double maxScore = 0;
    E value = null;

    for (E each : type.getEnumConstants()) {
      final double score = LiquidMetal.score(each.name(), text.replace(' ', '_'));
      if (score >= maxScore) {
        maxScore = score;
        value = each;
      }
      if (score >= 1) break;
    }

    if (maxScore < 0.25 || (!fuzzyMatch && maxScore < 1)) {
      throw invalidFormat(text, type, value.name().toLowerCase(), null);
    }

    if (!range.contains(value)) {
      throw outOfRange(text, range);
    }

    return value;
  }
}
