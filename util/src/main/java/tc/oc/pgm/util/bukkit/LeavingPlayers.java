package tc.oc.pgm.util.bukkit;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeavingPlayers implements Listener {
  private static final LeavingPlayers INSTANCE = new LeavingPlayers();

  private final Set<Player> recentlyDisconnected = new ReferenceOpenHashSet<>();

  private LeavingPlayers() {
    Bukkit.getPluginManager().registerEvents(this, BukkitUtils.getPlugin());
  }

  public static boolean contains(Player player) {
    return INSTANCE.recentlyDisconnected.contains(player);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (recentlyDisconnected.isEmpty())
      Bukkit.getScheduler()
          .scheduleSyncDelayedTask(BukkitUtils.getPlugin(), recentlyDisconnected::clear);
    recentlyDisconnected.add(event.getPlayer());
  }
}
