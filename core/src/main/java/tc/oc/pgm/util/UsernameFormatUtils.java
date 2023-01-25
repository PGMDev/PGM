package tc.oc.pgm.util;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.named.NameStyle;

/**
 * UsernameFormatUtils - A utility class with methods related to username formatting. Mainly used
 * for {@link ModerationCommand} but could be useful in other places.
 */
public class UsernameFormatUtils {

  public static final Component CONSOLE_NAME =
      translatable("misc.console", NamedTextColor.DARK_AQUA)
          .decoration(TextDecoration.ITALIC, true);

  public static Component formatStaffName(CommandSender sender, Match match) {
    if (sender != null && sender instanceof Player) {
      MatchPlayer matchPlayer = match.getPlayer((Player) sender);
      if (matchPlayer != null) return matchPlayer.getName(NameStyle.FANCY);
    }
    return CONSOLE_NAME;
  }
}
