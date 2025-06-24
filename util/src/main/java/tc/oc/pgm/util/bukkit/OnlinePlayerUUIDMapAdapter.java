package tc.oc.pgm.util.bukkit;

import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class OnlinePlayerUUIDMapAdapter<V> extends ListeningMapAdapter<UUID, V>
    implements Listener {
  public OnlinePlayerUUIDMapAdapter(Plugin plugin) {
    super(plugin);
  }

  public OnlinePlayerUUIDMapAdapter(Map<UUID, V> map, Plugin plugin) {
    super(map, plugin);
  }

  public boolean isValid(UUID key) {
    Player player = Bukkit.getPlayer(key);
    return player != null && player.isOnline();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.remove(event.getPlayer().getUniqueId());
  }
}
