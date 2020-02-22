package tc.oc.util.collection;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.util.*;

/**
 * Map adapter that inherits entries from a parent map. This map's entries have priority over the
 * parent's. Modifications affect this map only. Modification through views or iterators is not
 * supported.
 */
public class InheritingMap<K, V> extends ForwardingMap<K, V> {

  private final Map<K, V> map, parent;

  public InheritingMap(Map<K, V> parent) {
    this(new HashMap<K, V>(), parent);
  }

  public InheritingMap(Map<K, V> map, Map<K, V> parent) {
    this.map = map;
    this.parent = parent;
  }

  @SafeVarargs
  public static <K, V> Map<K, V> chain(Map<K, V>... maps) {
    return chain(Iterators.forArray(maps));
  }

  public static <K, V> Map<K, V> chain(Iterable<Map<K, V>> maps) {
    return chain(maps.iterator());
  }

  public static <K, V> Map<K, V> chain(Iterator<Map<K, V>> maps) {
    if (!maps.hasNext()) return Collections.emptyMap();
    final Map<K, V> head = maps.next();
    if (!maps.hasNext()) return head;
    return new InheritingMap<>(head, chain(maps));
  }

  @Override
  protected Map<K, V> delegate() {
    return map;
  }

  @Override
  public int size() {
    return Sets.union(map.keySet(), parent.keySet()).size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty() && parent.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key) || parent.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value) || parent.containsValue(value);
  }

  @Override
  public V get(Object key) {
    if (map.containsKey(key)) {
      return map.get(key);
    } else {
      return parent.get(key);
    }
  }

  @Override
  public Set<K> keySet() {
    return Sets.union(map.keySet(), parent.keySet());
  }

  @Override
  public Collection<V> values() {
    return Collections2.transform(
        keySet(),
        new Function<K, V>() {
          @Override
          public V apply(K key) {
            return get(key);
          }
        });
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    // Union of child's entrySet and paren't entrySet with child's keys filtered out
    return Sets.union(
        map.entrySet(),
        Sets.filter(
            parent.entrySet(),
            new Predicate<Map.Entry<K, V>>() {
              @Override
              public boolean apply(Map.Entry<K, V> entry) {
                return !map.containsKey(entry.getKey());
              }
            }));
  }
}
