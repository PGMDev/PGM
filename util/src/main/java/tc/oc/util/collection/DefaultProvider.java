package tc.oc.util.collection;

public interface DefaultProvider<K, V> {
  public V get(K key);
}
