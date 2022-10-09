package tc.oc.pgm.util;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Assert {
  /**
   * Asserts that a value is not null.
   *
   * @param <V> the value type
   * @param value the value
   * @param description the description of the value
   * @return the value
   */
  public static <V> @NotNull V assertNotNull(
      final @Nullable V value, final @NotNull String description) {
    if (value == null) {
      throw new NullPointerException(description);
    }
    return value;
  }

  /**
   * Asserts that a value is not null.
   *
   * @param <V> the value type
   * @param value the value
   * @return the value
   */
  public static <V> @NotNull V assertNotNull(final @Nullable V value) {
    return assertNotNull(value, "value cannot be null");
  }

  /**
   * Asserts that a value satisfies a condition.
   *
   * @param <V> the value type
   * @param value the value
   * @param conditon the condition to test the value
   * @param description the description of the condition
   * @return the value
   */
  public static <V> @NotNull V assertTrue(
      final @Nullable V value,
      final @NotNull Function<V, Boolean> conditon,
      final @NotNull String description) {
    if (!conditon.apply(assertNotNull(value, description))) {
      throw new IllegalArgumentException(description);
    }
    return value;
  }

  /**
   * Asserts that a condition returns true.
   *
   * @param conditon the condition
   * @param description the description of the condition
   */
  public static void assertTrue(final boolean condition, final @NotNull String description) {
    if (!condition) {
      throw new IllegalArgumentException(description);
    }
  }

  /**
   * Asserts that a condition returns true.
   *
   * @param conditon the condition
   */
  public static void assertTrue(final boolean condition) {
    assertTrue(condition, "condition did not return true");
  }
}
