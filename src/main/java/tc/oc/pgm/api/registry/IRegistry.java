package tc.oc.pgm.api.registry;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * A container that collects T by its ID as {@link String}.
 *
 * @param <T> type of objects to collect
 */
// TODO should be extended to NamespacedKeys
public interface IRegistry<T> {
  /**
   * Does this registry contain the given ID?
   *
   * @param id ID of the object
   * @return {@code true} whether this registry contains ID, otherwise {@link false}
   */
  boolean contains(String id);

  /**
   * Get T object from the given ID.
   *
   * @param id ID of the object
   * @return Object from this ID, never {@code null}
   * @throws NoSuchElementException When this registry does not contain the given ID
   */
  T get(String id) throws NoSuchElementException;

  /**
   * Get optional T object from the given ID.
   *
   * @param id ID of the object
   * @return Object from this ID, or {@link Optional#empty()} when this registry does not contain
   *     the given ID
   */
  Optional<T> getMaybe(String id);

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
   * Convert this registry into a {@link Map} where keys are string IDs.
   *
   * @return {@link Map} of all entries
   */
  Map<String, T> asKeyMap();

  /**
   * Register the given T object in this registry.
   *
   * @param id ID of the object
   * @param object Object itself, may not be {@code null}
   */
  void register(String id, T object);

  /**
   * Unregister object from this registry.
   *
   * @param id ID of the object
   * @return {@code true} whether this registry contained this ID, otherwise {@code false}
   */
  boolean unregister(String id);
}
