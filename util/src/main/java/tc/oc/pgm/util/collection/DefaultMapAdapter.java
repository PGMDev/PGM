package tc.oc.pgm.util.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Adapt a Map to return a default value for missing keys. The default value can be constant or
 * generated dynamically by a DefaultProvider. Only the get() method uses default values. All other
 * methods behave normally for missing keys.
 */
public class DefaultMapAdapter<K, V> implements Map<K, V> {

  private final Map<K, V> map;
  private final Function<? super K, ? extends V> defaultProvider;
  private final boolean putDefault;

  public DefaultMapAdapter(
      Map<K, V> map, Function<? super K, ? extends V> defaultProvider, boolean putDefault) {
    this.defaultProvider = defaultProvider;
    this.map = map;
    this.putDefault = putDefault;
  }

  public DefaultMapAdapter(Map<K, V> map, final V defaultValue, boolean putDefault) {
    this(map, key -> defaultValue, putDefault);
  }

  public DefaultMapAdapter(final V defaultValue, boolean putDefault) {
    this(new HashMap<>(), defaultValue, putDefault);
  }

  public DefaultMapAdapter(Function<? super K, ? extends V> defaultProvider, boolean putDefault) {
    this(new HashMap<>(), defaultProvider, putDefault);
  }

  public DefaultMapAdapter(Map<K, V> map, final V defaultValue) {
    this(map, defaultValue, false);
  }

  public DefaultMapAdapter(Map<K, V> map, Function<? super K, ? extends V> defaultProvider) {
    this(map, defaultProvider, false);
  }

  public DefaultMapAdapter(final V defaultValue) {
    this(defaultValue, false);
  }

  public DefaultMapAdapter(final V defaultValue, int initialSize, boolean putDefault) {
    this(new HashMap<>(initialSize), defaultValue, putDefault);
  }

  public DefaultMapAdapter(Function<? super K, ? extends V> defaultProvider) {
    this(defaultProvider, false);
  }

  @Override
  public V get(Object key) {
    V value = this.map.get(key);
    if (value == null) {
      value = this.defaultProvider.apply((K) key);
      if (this.putDefault) this.map.put((K) key, value);
    }
    return value;
  }

  public V getOrDefault(K key) {
    V value = this.map.get(key);
    if (value == null) {
      value = this.defaultProvider.apply(key);
    }
    return value;
  }

  public V getOrCreate(K key) {
    V value = this.map.get(key);
    if (value == null) {
      value = this.defaultProvider.apply(key);
      this.map.put(key, value);
    }
    return value;
  }

  public V getOrNull(K key) {
    return this.map.get(key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return map.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return map.equals(o);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  /**
   * If the given key was not previously in the map, the default value is returned, rather than
   * null. This is so that put(x, ...) always returns the same thing as get(x) for any given state.
   *
   * <p>{@link #putNoDefault} can be used if you want null returned for new keys, instead of the
   * default.
   */
  public V put(K key, V value) {
    V previous = map.put(key, value);
    return previous != null ? previous : this.defaultProvider.apply(key);
  }

  public V putNoDefault(K key, V value) {
    return map.put(key, value);
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public Collection<V> values() {
    return map.values();
  }
}
