package tc.oc.pgm.util;

import static tc.oc.pgm.util.player.PlayerComponent.player;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.named.NameStyle;

/**
 * UsernameFormatUtils - A utility class with methods related to username formatting. Was used for
 * staff related commands in the past, but is now deprecated.
 */
@Deprecated(forRemoval = true)
public class UsernameFormatUtils {

  public static Component formatStaffName(CommandSender sender) {
    return player(sender, NameStyle.FANCY);
  }
}
