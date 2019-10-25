package tc.oc.pgm.listeners;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.AllTranslations;
import tc.oc.world.OnlinePlayerMapAdapter;

public class InactivePlayerListener implements Listener {
  private static final String AFK_FOREVER_PERM = "afk.forever";

  private final Plugin plugin;
  private final Duration timeout;
  private final @Nullable Duration warning;

  private final OnlinePlayerMapAdapter<Instant> lastActivity;
  private Instant lastCheck;

  private class KickTask implements Runnable {
    @Override
    public void run() {
      Instant now = Instant.now();
      Instant kickTime = now.minus(timeout);
      Instant warnTime = warning == null ? null : now.minus(warning);
      Instant lastWarnTime = warning == null || lastCheck == null ? null : lastCheck.minus(warning);

      // Iterate over a copy, because kicking players while iterating the original
      // OnlinePlayerMapAdapter throws a ConcurrentModificationException
      for (Map.Entry<Player, Instant> entry : lastActivity.entrySetCopy()) {
        Player player = entry.getKey();
        Instant time = entry.getValue();

        if (time.isBefore(kickTime)) {
          player.kickPlayer(AllTranslations.get().translate("afk.kick", player));
        } else if (warnTime != null && time.isAfter(lastWarnTime) && !time.isAfter(warnTime)) {
          player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1);
          player.sendMessage(
              ChatColor.RED.toString()
                  + ChatColor.BOLD
                  + AllTranslations.get()
                      .translate(
                          "afk.warn",
                          player,
                          ChatColor.AQUA.toString()
                              + ChatColor.BOLD
                              + timeout.minus(warning).getStandardSeconds()
                              + ChatColor.RED
                              + ChatColor.BOLD));
        }
      }

      lastCheck = now;
    }
  }

  public InactivePlayerListener(
      Plugin plugin, Duration timeout, @Nullable Duration warning, Duration interval) {
    this.plugin = plugin;
    this.timeout = timeout;
    this.warning = warning;

    this.lastActivity = new OnlinePlayerMapAdapter<>(plugin);
    this.lastActivity.enable();

    long tickInterval = interval.getMillis() / 50;
    this.plugin
        .getServer()
        .getScheduler()
        .runTaskTimer(this.plugin, new KickTask(), tickInterval, tickInterval);
  }

  private void activity(Player player) {
    if (player.hasPermission(AFK_FOREVER_PERM)) {
      this.lastActivity.remove(player);
    } else {
      this.lastActivity.put(player, Instant.now());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void join(PlayerJoinEvent event) {
    this.activity(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void move(PlayerMoveEvent event) {
    if (!Objects.equals(event.getFrom(), event.getTo())) {
      this.activity(event.getPlayer());
    }
  }
}
