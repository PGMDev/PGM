package tc.oc.pgm.listeners;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;

public class JoinLeaveAnnouncer implements Listener {

  private final MatchManager mm;

  public JoinLeaveAnnouncer(MatchManager mm) {
    this.mm = mm;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void announceJoin(final PlayerJoinEvent event) {
    MatchPlayer player = this.mm.getPlayer(event.getPlayer());
    if (player == null) return;

    if (event.getJoinMessage() != null) {
      event.setJoinMessage(null);
      join(player, JoinVisibility.get(player));
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void announceLeave(PlayerQuitEvent event) {
    MatchPlayer player = this.mm.getPlayer(event.getPlayer());
    if (player == null) return;

    if (event.getQuitMessage() != null) {
      event.setQuitMessage(null);
      leave(player, JoinVisibility.get(player));
    }
  }

  @EventHandler
  public void onPlayerVanish(PlayerVanishEvent event) {
    MatchPlayer player = event.getPlayer();
    if (player == null) return;
    if (event.isQuiet()) return;

    if (event.isVanished()) {
      leave(player, JoinVisibility.NONSTAFF);
    } else {
      join(player, JoinVisibility.NONSTAFF);
    }
  }

  public void join(MatchPlayer player, JoinVisibility visibility) {
    handleJoinLeave(player, "misc.join", visibility);
  }

  public void leave(MatchPlayer player, JoinVisibility visibility) {
    handleJoinLeave(player, "misc.leave", visibility);
  }

  private void handleJoinLeave(MatchPlayer player, String key, JoinVisibility visibility) {
    boolean staff = visibility == JoinVisibility.STAFF;

    Collection<MatchPlayer> viewers = player.getMatch().getPlayers();
    for (MatchPlayer viewer : viewers) {
      if (player.equals(viewer)) continue; // Never display own broadcast

      if (canView(viewer, player, visibility)) { // Check if viewer setting allows join/leaves

        Component component =
            translatable(key + (staff ? ".quiet" : ""), NamedTextColor.YELLOW, player.getName());

        if (staff) {
          component = text().append(ChatDispatcher.ADMIN_CHAT_PREFIX).append(component).build();
        }

        viewer.sendMessage(component);
      }
    }
  }

  private boolean canView(MatchPlayer viewer, MatchPlayer target, JoinVisibility visibility) {
    boolean isStaff = viewer.getBukkit().hasPermission(Permissions.STAFF);

    SettingValue option = viewer.getSettings().getValue(SettingKey.JOIN);
    boolean allowed =
        option.equals(SettingValue.JOIN_ON)
            || areFriends(option, viewer.getBukkit(), target.getBukkit());

    if (!allowed) return false;

    switch (visibility) {
      case NONSTAFF:
        return !isStaff;
      case STAFF:
        return isStaff;
      default:
        return true;
    }
  }

  private boolean areFriends(SettingValue value, Player a, Player b) {
    return value.equals(SettingValue.JOIN_FRIENDS) && Integration.isFriend(a, b);
  }

  public static enum JoinVisibility {
    ALL, // When player is not vanished, show everyone
    STAFF, // When player is vanished and actually joins/quits, show staff only
    NONSTAFF; // When player toggles vanish, show non-staff (fake broadcast)

    public static JoinVisibility get(MatchPlayer player) {
      return Integration.isVanished(player.getBukkit()) ? STAFF : ALL;
    }
  }
}
