package tc.oc.util.collection;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/** {@link SetMultimap} that preserves the insertion order of values */
public class LinkedHashMultimap<K, V> extends ForwardingSetMultimap<K, V> {

  private final SetMultimap<K, V> delegate =
      Multimaps.newSetMultimap(
          new HashMap<K, Collection<V>>(),
          new Supplier<Set<V>>() {
            @Override
            public Set<V> get() {
              return new LinkedHashSet<V>();
            }
          });

  @Override
  protected SetMultimap<K, V> delegate() {
    return delegate;
  }

  /**
   * Add the given key-value pair to the map, forcing the value to the finish of the key's list if
   * it is already present.
   */
  public boolean force(K key, V value) {
    boolean added = !remove(key, value);
    put(key, value);
    return added;
  }
}
