package tc.oc.util.bukkit.world;

import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

/** Uses {@link ListeningMapAdapter} to guarantee that the map only contains alive entity. */
public class LivingEntityMapAdapter<V> extends ListeningMapAdapter<LivingEntity, V>
    implements Listener {

  public LivingEntityMapAdapter(Plugin plugin) {
    super(new WeakHashMap<LivingEntity, V>(), plugin);
  }

  @Override
  public boolean isValid(LivingEntity key) {
    return key != null && !key.isDead();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    this.remove(event.getEntity());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    for (Entity entity : event.getWorld().getEntities()) {
      map.remove(entity);
    }
  }

  protected Map<LivingEntity, V> delegate() {
    return this.map;
  }
}
