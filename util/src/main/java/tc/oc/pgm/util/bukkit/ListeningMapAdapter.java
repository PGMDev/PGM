package tc.oc.pgm.util.bukkit;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * {@link Map} adapter that uses {@link K} keys and guarantees that the map only ever contains valid
 * entries defined by the {@link #isValid(K)} method, subclasses override that method to define what
 * keys are valid. Subclass are also responsible for defining the events and their actions when the
 * adapter is enabled. MapAdapter is a {@link org.bukkit.event.Listener} and registers itself to
 * receive events on behalf of the plugin passed to the constructor when {@link #enable()} is
 * called. This must be called before using the map. The map can be unregistered by calling {@link
 * #disable()}.
 */
public abstract class ListeningMapAdapter<K, V> extends ForwardingMap<K, V> implements Listener {

  protected final Plugin plugin;
  protected final Map<K, V> map;
  protected boolean enabled;
  protected boolean lazyEnable = true;

  public ListeningMapAdapter(Plugin plugin) {
    this(new HashMap<K, V>(), plugin);
  }

  public ListeningMapAdapter(Map<K, V> map, Plugin plugin) {
    this.plugin = plugin;
    this.map = map;
  }

  protected void assertEnabled() {
    if (!this.enabled) {
      if (this.lazyEnable) {
        this.enable();
      } else {
        throw new IllegalStateException(
            "This " + this.getClass().getSimpleName() + " is not enabled");
      }
    }
  }

  /**
   * If the given entry is valid, add it to the map and return any previous value for that entry, or
   * null if they were previously not in the map. If the entry is not valid, no change is made to
   * the map and null is returned.
   */
  @Override
  public V put(K key, V value) {
    this.assertEnabled();
    if (isValid(key)) {
      return this.map.put(key, value);
    } else {
      return null;
    }
  }

  /** If the entry is a valid new entry */
  public abstract boolean isValid(K key);

  /**
   * Add an entry to the map for the given key and return any previous value for that entity, or
   * null if they were previously not in the map. The entry is added ignoring the checks.
   */
  public V force(K key, V value) {
    this.assertEnabled();
    return this.map.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> otherMap) {
    for (Map.Entry<? extends K, ? extends V> entry : otherMap.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  /** Special case of putAll that does not check if each entry is valid */
  public void putAll(ListeningMapAdapter<? extends K, ? extends V> otherMap) {
    this.assertEnabled();
    this.map.putAll(otherMap);
  }

  /** Register to receive events. This must be called before adding any entries to the map. */
  public void enable() {
    if (!this.enabled) {
      this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
      this.enabled = true;
    }
  }

  /**
   * Clear the list and stop listening for events. This map should not be used after this method is
   * called.
   */
  public void disable() {
    if (this.enabled) {
      this.clear();
      HandlerList.unregisterAll(this);
    }
  }

  /**
   * Return an immutable copy of this container's {@link #entrySet} that is safe to iterate over
   * while the container is modified, which tends to happen unexpectedly from events the container
   * is listening to.
   */
  public ImmutableSet<Map.Entry<K, V>> entrySetCopy() {
    return ImmutableSet.copyOf(this.entrySet());
  }

  @Override
  protected Map<K, V> delegate() {
    return this.map;
  }
}
