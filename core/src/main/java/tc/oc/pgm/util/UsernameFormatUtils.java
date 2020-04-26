package tc.oc.pgm.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

/**
 * UsernameFormatUtils - A utility class with methods related to username formatting. Mainly used
 * for {@link ModerationCommands} but could be useful in other places.
 */
public class UsernameFormatUtils {

  private static final Component CONSOLE_NAME =
      new PersonalizedTranslatable("console")
          .getPersonalizedText()
          .color(ChatColor.DARK_AQUA)
          .italic(true);

  public static Component formatStaffName(CommandSender sender, Match match) {
    if (sender != null && sender instanceof Player) {
      MatchPlayer matchPlayer = match.getPlayer((Player) sender);
      if (matchPlayer != null) return matchPlayer.getStyledName(NameStyle.FANCY);
    }
    return CONSOLE_NAME;
  }
}
