package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

/** Uses {@link ListeningMapAdapter} to guarantee that the map only contains online players. */
public class OnlinePlayerMapAdapter<V> extends ListeningMapAdapter<Player, V> implements Listener {

  public OnlinePlayerMapAdapter(Plugin plugin) {
    super(plugin);
  }

  public OnlinePlayerMapAdapter(Map<Player, V> map, Plugin plugin) {
    super(map, plugin);
  }

  @Override
  public boolean isValid(Player key) {
    return key.isOnline() && PLAYER_UTILS.willBeOnline(key);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.remove(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    V value = this.remove(event.getPlayer());
    if (value != null) {
      this.put(event.getPlayer(), value);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    this.keySet().removeIf(player -> player.getWorld().equals(event.getWorld()));
  }

  @Override
  protected Map<Player, V> delegate() {
    return this.map;
  }
}
