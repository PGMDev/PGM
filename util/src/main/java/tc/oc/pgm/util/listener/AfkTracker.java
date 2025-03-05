package tc.oc.pgm.util.listener;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

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
    softTrack(player, 1000);
  }

  private void softTrack(Player player, int amt) {
    var activity = getActivity(player);
    // Soft activity timed out, reset
    if (activity.softActive.until(now, ChronoUnit.SECONDS) >= 5) {
      activity.softActive = now;
      activity.softCount = 0;
    }
    if ((activity.softCount += amt) > 15) {
      activity.lastActive = activity.softActive = now;
      activity.softCount = 0;
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    track(event.getPlayer());
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event.getFrom().getYaw() == event.getTo().getYaw()
        || event.getFrom().getPitch() == event.getTo().getPitch()) return;
    softTrack(event.getPlayer(), 3);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCoarseMove(PlayerCoarseMoveEvent event) {
    // Ignore just falling or jumping
    Location to = event.getBlockTo(), from = event.getBlockFrom();
    if (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()) return;
    softTrack(event.getPlayer(), 1);
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
    // Ignore standing on pressure plates, only track right/left click
    if (event.getAction() == Action.PHYSICAL) return;
    track(event.getPlayer());
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    // Don't full track, as pgm could make you open stuff like team picker while afk
    if (event.getPlayer() instanceof Player p) softTrack(p, 3);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player p) track(p);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    // Don't full track, as pgm closes any inventory on cycle
    if (event.getPlayer() instanceof Player p) softTrack(p, 3);
  }

  public class Activity {
    private Instant lastActive = now;
    private Instant softActive = now;
    private int softCount = 0;

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
