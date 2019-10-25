package tc.oc.util;

import com.google.common.collect.Range;

public class Numbers {

  /**
   * Get the value of the given numeric type that best represents positive infinity.
   *
   * @throws ReflectiveOperationException if this fails, which should not happen with the primitive
   *     types
   */
  public static <T extends Number> T positiveInfinity(Class<T> type)
      throws ReflectiveOperationException {
    try {
      return type.cast(type.getField("POSITIVE_INFINITY").get(null));
    } catch (NoSuchFieldException e) {
      return type.cast(type.getField("MAX_VALUE").get(null));
    }
  }

  /**
   * Get the value of the given numeric type that best represents negative infinity.
   *
   * @throws ReflectiveOperationException if this fails, which should not happen with the primitive
   *     types
   */
  public static <T extends Number> T negativeInfinity(Class<T> type)
      throws ReflectiveOperationException {
    try {
      return type.cast(type.getField("NEGATIVE_INFINITY").get(null));
    } catch (NoSuchFieldException e) {
      return type.cast(type.getField("MIN_VALUE").get(null));
    }
  }

  /**
   * Try to parse the given text as a number of the given type
   *
   * @param text string representation of a number
   * @param type numeric type to parse
   * @param infinity whether infinities should be allowed
   * @return a parsed number
   * @throws NumberFormatException if a number could not be parsed for whatever reason
   */
  public static <T extends Number> T parse(String text, Class<T> type, boolean infinity)
      throws NumberFormatException {
    try {
      if (infinity) {
        String trimmed = text.trim();
        if ("oo".equals(trimmed) || "+oo".equals(trimmed)) {
          return positiveInfinity(type);
        } else if ("-oo".equals(trimmed)) {
          return negativeInfinity(type);
        }
      }
      return type.cast(type.getMethod("valueOf", String.class).invoke(null, text));
    } catch (ReflectiveOperationException e) {
      if (e.getCause() instanceof NumberFormatException) {
        throw (NumberFormatException) e.getCause();
      } else {
        throw new IllegalArgumentException("cannot parse type " + type.getName(), e);
      }
    }
  }

  public static <T extends Number> T coerce(Object obj, Class<T> type, boolean infinity)
      throws NumberFormatException {
    if (type.isInstance(obj)) {
      return type.cast(obj);
    } else if (obj instanceof String) {
      return parse((String) obj, type, infinity);
    } else if (obj instanceof Number) {
      Number n = (Number) obj;
      if (type.equals(Double.class)) {
        return type.cast(n.doubleValue());
      } else if (type.equals(Float.class)) {
        return type.cast(n.floatValue());
      } else if (type.equals(Long.class)) {
        return type.cast(n.longValue());
      } else if (type.equals(Integer.class)) {
        return type.cast(n.intValue());
      } else if (type.equals(Short.class)) {
        return type.cast(n.shortValue());
      } else if (type.equals(Byte.class)) {
        return type.cast(n.byteValue());
      }
    }

    throw new NumberFormatException("Cannot coerce " + obj + " to " + type.getSimpleName());
  }

  public static double clamp(double value, double min, double max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static int clamp(int value, int min, int max) {
    return value < min ? min : (value > max ? max : value);
  }

  public static double clamp(double value, Range<Double> range) {
    return clamp(
        value,
        range.hasLowerBound() ? range.lowerEndpoint() : Double.NEGATIVE_INFINITY,
        range.hasUpperBound() ? range.upperEndpoint() : Double.POSITIVE_INFINITY);
  }

  /**
   * Divide the first argument by the second and round the result up to the next integer.
   *
   * @param numerator Assumed to be >= 0
   * @param denominator Assumed to be > 0
   */
  public static int divideRoundingUp(int numerator, int denominator) {
    return (numerator + denominator - 1) / denominator;
  }

  /**
   * Round the first argument up to the next multiple of the second argument
   *
   * @param value Assumed to be >= 0
   * @param modulus Assumed to be > 0
   */
  public static int roundUp(int value, int modulus) {
    return divideRoundingUp(value, modulus) * modulus;
  }

  /**
   * Round the first argument up to the next multiple of the second argument
   *
   * @param value Assumed to be >= 0
   * @param modulus Assumed to be > 0
   */
  public static int roundDown(int value, int modulus) {
    return (value / modulus) * modulus;
  }

  /**
   * Convert a value in the range 0..1 to a percentage in the range 0..100. The result will only be
   * 0 if the input is exactly 0, and will only be 100 if the input is exactly 1.
   */
  public static int percentage(double n) {
    int percent = (int) Math.round(n * 100);
    if (percent == 0 && n != 0) {
      percent = 1;
    } else if (percent == 100 && n != 1) {
      percent = 99;
    }
    return percent;
  }
}
