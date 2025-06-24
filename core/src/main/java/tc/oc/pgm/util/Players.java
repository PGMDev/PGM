package tc.oc.pgm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class Players {

  public static boolean isVisible(CommandSender viewer, Player other) {
    return !Integration.isVanished(other) || viewer.hasPermission(Permissions.VANISH);
  }

  public static boolean shouldReveal(CommandSender viewer, Player other) {
    return viewer == other
        || (isVisible(viewer, other)
            && (Integration.getNick(other) == null
                || isFriend(viewer, other)
                || viewer.hasPermission(Permissions.STAFF)));
  }

  public static String getVisibleName(CommandSender viewer, Player other) {
    String nick = Integration.getNick(other);
    if (nick == null || isFriend(viewer, other) || viewer.hasPermission(Permissions.STAFF))
      return other.getName();
    return nick;
  }

  public static List<String> getPlayerNames(CommandSender sender, String query) {
    query = query.toLowerCase(Locale.ROOT);
    List<String> matches = new ArrayList<>();
    boolean anyDirect = false;
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (!Players.isVisible(sender, p)) continue;
      String n = Players.getVisibleName(sender, p);
      if (n.toLowerCase(Locale.ROOT).startsWith(query)) {
        if (!anyDirect) {
          anyDirect = true;
          matches.clear();
        }
        matches.add(n);
      } else if (!anyDirect && LiquidMetal.match(n, query)) {
        matches.add(n);
      }
    }
    return matches;
  }

  public static Player getPlayer(CommandSender sender, String query) {
    return StringUtils.bestFuzzyMatch(
        query,
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> Players.isVisible(sender, p))
            .iterator(),
        p -> Players.getVisibleName(sender, p));
  }

  public static MatchPlayer getMatchPlayer(CommandSender sender, String query) {
    return PGM.get().getMatchManager().getPlayer(getPlayer(sender, query));
  }

  public static boolean isFriend(CommandSender viewer, Player other) {
    return viewer instanceof Player && Integration.isFriend((Player) viewer, other);
  }

  public static List<String> suggestPlayers(
      CommandContext<CommandSender> context, CommandInput input) {
    var query = input.lastRemainingToken();
    var suggestions = getPlayerNames(context.sender(), query);
    // Typing "/g Hello Us" suggests "Hello User". Without the prefix, cloud refuses to suggest it.
    var prefix = input.read(input.remainingLength() - query.length());
    suggestions.replaceAll(str -> prefix + str);
    return suggestions;
  }
}
