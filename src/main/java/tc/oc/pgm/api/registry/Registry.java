package tc.oc.pgm.api.registry;

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class Registry<T> implements IRegistry<T> {
  private final Map<String, T> map;

  public Registry(Map<String, T> map) {
    this.map = checkNotNull(map);
  }

  @Override
  public boolean contains(String id) {
    return map.containsKey(checkNotNull(id));
  }

  @Override
  public T get(String id) throws NoSuchElementException {
    return getMaybe(checkNotNull(id)).orElseThrow(NoSuchElementException::new);
  }

  @Override
  public Optional<T> getMaybe(String id) {
    return Optional.ofNullable(map.get(checkNotNull(id)));
  }

  @Override
  public Set<String> getKeys() {
    return map.keySet();
  }

  @Override
  public Collection<T> getAll() {
    return map.values();
  }

  @Override
  public Set<Map.Entry<String, T>> entrySet() {
    return map.entrySet();
  }

  @Override
  public Map<String, T> asKeyMap() {
    return Collections.unmodifiableMap(map);
  }

  @Override
  public void register(String id, T object) {
    checkNotNull(id);
    checkNotNull(object);
    map.put(id, object);
  }

  @Override
  public boolean unregister(String id) {
    return map.remove(checkNotNull(id)) != null;
  }
}
