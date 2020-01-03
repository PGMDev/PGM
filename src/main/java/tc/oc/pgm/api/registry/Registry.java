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
  public boolean contains(String key) {
    return map.containsKey(checkNotNull(key));
  }

  @Override
  public T get(String key) throws NoSuchElementException {
    return getMaybe(checkNotNull(key)).orElseThrow(NoSuchElementException::new);
  }

  @Override
  public Optional<T> getMaybe(String key) {
    return Optional.ofNullable(map.get(checkNotNull(key)));
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
  public boolean register(String key, T object) {
    checkNotNull(key);
    checkNotNull(object);
    return map.putIfAbsent(key, object) == null;
  }

  @Override
  public boolean unregister(String key) {
    return map.remove(checkNotNull(key)) != null;
  }
}
