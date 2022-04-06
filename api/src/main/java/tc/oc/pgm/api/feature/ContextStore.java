package tc.oc.pgm.api.feature;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public interface ContextStore<T> {
  Iterator<Map.Entry<String, T>> iterator();

  boolean contains(String name);

  String add(T obj);

  void add(String name, T obj);

  T get(String name);

  String getName(T obj);

  Collection<T> getAll();

  @SuppressWarnings("unchecked")
  <V extends T> Collection<V> getAll(Class<V> clazz);
}
