package tc.oc.pgm.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;

public class Players {

  public static boolean shouldShow(CommandSender viewer, Player other) {
    return !Integration.isVanished(other) && viewer.hasPermission(Permissions.VANISH);
  }

  public static String getVisibleName(CommandSender viewer, Player other) {
    String nick = Integration.getNick(other);
    if (nick != null
        && viewer instanceof Player
        && !Integration.isFriend((Player) viewer, other)
        && !viewer.hasPermission(Permissions.STAFF)) return other.getName();
    return nick;
  }
}
