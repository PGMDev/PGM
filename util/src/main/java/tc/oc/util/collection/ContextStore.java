package tc.oc.util.collection;

import com.google.common.collect.Maps;
import java.util.*;
import java.util.Map.Entry;

public class ContextStore<T> implements Iterable<Entry<String, T>> {
  protected final Map<String, T> store = Maps.newLinkedHashMap();

  @Override
  public Iterator<Entry<String, T>> iterator() {
    return this.store.entrySet().iterator();
  }

  public boolean contains(String name) {
    return store.containsKey(name);
  }

  /**
   * Adds an object to this context with a random name.
   *
   * @param obj Object to add.
   * @return Randomly generated name for this object.
   */
  public String add(T obj) {
    while (true) {
      String name = UUID.randomUUID().toString();
      try {
        this.add(name, obj);
        return name;
      } catch (IllegalArgumentException e) {
        // ignore, try to find another name that works
      }
    }
  }

  /**
   * Adds an object to this context with a specified name.
   *
   * @param name Name for this object.
   * @param obj Object to add.
   */
  public void add(String name, T obj) {
    T old = store.put(name, obj);
    if (old != null && old != obj) {
      store.put(name, old);
      throw new IllegalArgumentException(
          "ContextStore already has an object assigned to '" + name + "'");
    }
  }

  /**
   * Gets an object by the name it was registered with.
   *
   * @param name Name to look up.
   * @return Object that was registered to the given name or null if none exists.
   */
  public T get(String name) {
    return this.store.get(name);
  }

  /**
   * Gets the first name the specified object was registered with.
   *
   * @param obj Object to look up.
   * @return Name for the object or null if none is found.
   * @note This method will look up the exact equality operator then the .equals method.
   */
  public String getName(T obj) {
    for (Entry<String, T> entry : this.store.entrySet()) {
      if (entry.getValue() == obj || entry.getValue().equals(obj)) {
        return entry.getKey();
      }
    }
    return null;
  }

  public Collection<T> getAll() {
    return this.store.values();
  }

  @SuppressWarnings("unchecked")
  public <V extends T> Collection<V> getAll(Class<V> clazz) {
    Set<V> results = new HashSet<>();

    for (T t : this.getAll()) {
      if (clazz.isAssignableFrom(t.getClass())) {
        results.add((V) t);
      }
    }

    return results;
  }
}
