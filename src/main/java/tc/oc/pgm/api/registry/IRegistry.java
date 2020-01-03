package tc.oc.pgm.api.registry;

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * A container that collects T by its key as {@link String}.
 *
 * @param <T> type of objects to collect
 */
// TODO should be extended to NamespacedKeys
public interface IRegistry<T> extends Iterable<T> {

  /**
   * Does this registry contain the given key?
   *
   * @param key key of the object
   * @return {@code true} whether this registry contains key, otherwise {@link false}
   */
  boolean contains(String key);

  /**
   * Get T object from the given key.
   *
   * @param key key of the object
   * @return object from this key, never {@code null}
   * @throws NoSuchElementException when this registry does not contain the given key
   */
  T get(String key) throws NoSuchElementException;

  /**
   * Get optional T object from the given key.
   *
   * @param key key of the object
   * @return object from this key, or {@link Optional#empty()} when this registry does not contain
   *     the given key
   */
  Optional<T> getMaybe(String key);

  /**
   * Get all keys registered in this registry.
   *
   * @return {@link Set} of all keys
   */
  Set<String> getKeys();

  /**
   * Get all T objects registered in this registry.
   *
   * @return {@link Collection} of all objects
   */
  Collection<T> getAll();

  /**
   * Convert this registry into a {@link Set} of entries.
   *
   * @return {@link Set} of all entries
   */
  Set<Map.Entry<String, T>> entrySet();

  /**
   * Convert this registry into a {@link Map} where keys are string keys.
   *
   * @return {@link Map} of all entries
   */
  Map<String, T> asKeyMap();

  /**
   * Register the given T object in this registry.
   *
   * @param key key of the object
   * @param object Object itself, may not be {@code null}
   * @return {@code true} whether registration was successful, otherwise {@code false}
   */
  boolean register(String key, T object);

  /**
   * Unregister object from this registry.
   *
   * @param key key of the object
   * @return {@code true} whether this registry contained this key, otherwise {@code false}
   */
  boolean unregister(String key);

  @Override
  default Iterator<T> iterator() {
    return getAll().iterator();
  }

  /**
   * Format the given key to the vanilla namespaced key standard.
   * https://minecraft.gamepedia.com/Namespaced_ID
   *
   * @param key key to format
   * @return formatted key
   * @deprecated This method is meant to be used only by the PGM plugin!
   */
  static String formatPGMKey(String key) {
    return "pgm:" + checkNotNull(key);
  }
}
