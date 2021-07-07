package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.PlayerComponentProvider.CONSOLE;
import static tc.oc.pgm.util.text.PlayerComponentProvider.UNKNOWN;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;

/** PlayerComponent is used to format player names in a consistent manner with optional styling */
public final class PlayerComponent {
  private PlayerComponent() {}

  public static Component player(UUID playerId, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null ? player(player, style) : UNKNOWN;
  }

  public static Component player(UUID playerId, String defName, NameStyle style) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null
        ? player(player, style)
        : defName != null ? player(player, defName, style) : UNKNOWN;
  }

  public static Component player(CommandSender sender, NameStyle style) {
    if (sender instanceof Player) {
      return player((Player) sender, style);
    } else {
      return CONSOLE;
    }
  }

  public static Component player(Player player, NameStyle style) {
    return player(player, "", style);
  }

  public static Component player(Player player, String defName, NameStyle style) {
    String prefix = player != null ? "@" : "!";
    return text(
        "<"
            + prefix
            + (player != null ? player.getUniqueId().toString() : defName)
            + ":"
            + style.ordinal()
            + ">");
  }
}
