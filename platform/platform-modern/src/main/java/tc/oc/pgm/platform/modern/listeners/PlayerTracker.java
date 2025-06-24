package tc.oc.pgm.platform.modern.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerTracker implements Listener {
  private final Int2ReferenceOpenHashMap<Player> playerEntities = new Int2ReferenceOpenHashMap<>();
  private final Reference2IntOpenHashMap<UUID> byUuid = new Reference2IntOpenHashMap<>();

  public PlayerTracker() {
    byUuid.defaultReturnValue(-1);
  }

  public Player get(int entityId) {
    return playerEntities.get(entityId);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    register(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(EntityAddToWorldEvent event) {
    if (event.getEntity() instanceof Player pl) register(pl);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent player) {
    cleanup(player.getPlayer());
  }

  private void cleanup(Player player) {
    int oldId = byUuid.removeInt(player.getUniqueId());
    if (oldId != -1) playerEntities.remove(oldId);
    int newId = player.getEntityId();
    if (newId != oldId) playerEntities.remove(newId);
  }

  private void register(Player player) {
    int entityId = player.getEntityId();
    cleanup(player);

    byUuid.put(player.getUniqueId(), entityId);
    playerEntities.put(entityId, player);
  }
}
