package tc.oc.pgm.util;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;

/**
 * Match-scoped values attached to entities, keyed by UUID. Values are dropped once the entity is
 * destroyed.
 */
public class EntityMap<V> implements Listener {

  private final Match match;
  private final Map<UUID, V> values = new HashMap<>();

  public EntityMap(Match match) {
    this.match = match;
    match.addListener(this, MatchScope.LOADED);
  }

  public @Nullable V get(Entity entity) {
    return values.get(entity.getUniqueId());
  }

  public boolean contains(Entity entity) {
    return values.containsKey(entity.getUniqueId());
  }

  public void put(Entity entity, V value) {
    values.put(entity.getUniqueId(), value);
  }

  public @Nullable V remove(Entity entity) {
    return values.remove(entity.getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityRemove(EntityRemoveFromWorldEvent event) {
    UUID uuid = event.getEntity().getUniqueId();
    if (!values.containsKey(uuid) || !MISC_UTILS.isEntityDestroyed(event)) return;

    // Deferred so anything resolving the entity later this tick still finds it
    match.getExecutor(MatchScope.LOADED).execute(() -> values.remove(uuid));
  }
}
