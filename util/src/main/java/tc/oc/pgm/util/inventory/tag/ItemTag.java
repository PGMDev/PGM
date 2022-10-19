package tc.oc.pgm.util.inventory.tag;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A persistent data holder for {@link ItemStack}s. */
public interface ItemTag<T> {

  /**
   * Gets the value, or null if not present.
   *
   * @param item An item.
   * @return A value or null.
   */
  @Nullable
  T get(ItemStack item);

  /**
   * Gets whether a value is present.
   *
   * @param item An item.
   * @return If a value is present.
   */
  default boolean has(ItemStack item) {
    return get(item) != null;
  }

  /**
   * Sets the value.
   *
   * @param item An item.
   * @param value A value.
   */
  void set(ItemStack item, T value);

  /**
   * Clears the value.
   *
   * @param item An item.
   */
  void clear(ItemStack item);

  /**
   * Creates a string item tag.
   *
   * @param key A key.
   * @return An item tag.
   */
  static ItemTag<String> newString(String key) {
    // TODO: Add an implementation that uses the 1.14+ org.bukkit.persistence API
    return new LegacyItemTag();
  }

  /**
   * Creates a boolean item tag.
   *
   * @param key A key.
   * @return An item tag.
   */
  static ItemTag<Boolean> newBoolean(String key) {
    return new BooleanItemTag(newString(key));
  }
}
