package tc.oc.pgm.features;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.feature.MatchFeatureContext;

public class MatchFeatureContextImpl implements MatchFeatureContext {
  protected final Map<String, Feature> store = Maps.newTreeMap();

  @Override
  public Iterator<Map.Entry<String, Feature>> iterator() {
    return this.store.entrySet().iterator();
  }

  @Override
  public boolean contains(String name) {
    return store.containsKey(name);
  }

  /**
   * Adds an object to this context with a random name.
   *
   * @param feature Object to add.
   * @return Randomly generated name for this object.
   */
  @Override
  public String add(Feature feature) {
    this.add(feature.getId(), feature);
    return feature.getId();
  }

  @Override
  public <T extends Feature> T get(String id, Class<T> type) {
    return (T) this.get(id);
  }

  /**
   * Adds an object to this context with a specified name.
   *
   * @param name Name for this object.
   * @param obj Object to add.
   */
  @Override
  public void add(String name, Feature obj) {
    Feature old = store.put(name, obj);
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
  @Override
  public Feature get(String name) {
    return this.store.get(name);
  }

  /**
   * Gets the first name the specified object was registered with.
   *
   * @param obj Object to look up.
   * @return Name for the object or null if none is found.
   * @note This method will look up the exact equality operator then the .equals method.
   */
  @Override
  public String getName(Feature obj) {
    for (Map.Entry<String, Feature> entry : this.store.entrySet()) {
      if (entry.getValue() == obj || entry.getValue().equals(obj)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public Collection<Feature> getAll() {
    return this.store.values();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V extends Feature> Collection<V> getAll(Class<V> clazz) {
    Set<V> results = new HashSet<>();

    for (Feature t : this.getAll()) {
      if (clazz.isAssignableFrom(t.getClass())) {
        results.add((V) t);
      }
    }

    return results;
  }
}
