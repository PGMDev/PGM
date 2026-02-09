package tc.oc.pgm.util.bukkit;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface GameRule<T> {
  /**
   * Gets the name of the game rule
   *
   * @return The name of the game rule
   */
  String name();

  Class<T> type();

  /**
   * Gets the value of the game rule in the given world
   *
   * @param world The world to get the game rule from
   * @return The value of the game rule, or null if not set
   */
  @Nullable
  T get(World world);

  /**
   * Sets the value of the game rule in the given world
   *
   * @param world The world to set the game rule in
   * @param value The value to set, or null to reset to default
   */
  void set(World world, @Nullable T value);

  /**
   * Tries to parse the given string into a value through the implementation's parsing logic
   *
   * @param value The string value to parse
   * @return The parsed game rule value
   * @throws Throwable if an error occurs during parsing.
   */
  T tryParse(String value) throws Throwable;

  /**
   * Checks if this game rule can be combined with another game rule
   *
   * @param other The other game rule to check compatibility with
   * @return True if the game rules can be combined, false otherwise
   */
  boolean canBeCombinedWith(GameRule<?> other);
}
