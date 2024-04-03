package tc.oc.pgm.util.listener;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;

public class AfkTracker implements Listener {

  private final Map<Player, Activity> activityMap;

  // By recycling cached instants we avoid creating a ton of objects.
  private Instant now = Instant.now();

  public AfkTracker(JavaPlugin plugin) {
    this.activityMap = new OnlinePlayerMapAdapter<>(plugin);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> now = Instant.now(), 0L, 5L);
  }

  public Activity getActivity(Player player) {
    return activityMap.computeIfAbsent(player, pl -> new Activity());
  }

  private void track(Player player) {
    getActivity(player).lastActive = now;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    track(event.getPlayer());
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event.getFrom().getYaw() != event.getTo().getYaw()
        && event.getFrom().getPitch() != event.getTo().getPitch()) {
      track(event.getPlayer());
    }
  }

  @EventHandler
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    track(event.getPlayer());
  }

  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent event) {
    track(event.getPlayer());
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    track(event.getPlayer());
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (event.getPlayer() instanceof Player) track((Player) event.getPlayer());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player) track((Player) event.getWhoClicked());
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player) track((Player) event.getPlayer());
  }

  public class Activity {
    private Instant lastActive = now;

    public Instant getLastActive() {
      return lastActive;
    }

    public Duration getAfkDuration() {
      return Duration.between(lastActive, now);
    }

    public boolean isAfk(Duration duration) {
      return getAfkDuration().compareTo(duration) >= 0;
    }

    public boolean isActive(Duration duration) {
      return getAfkDuration().compareTo(duration) < 0;
    }
  }
}
