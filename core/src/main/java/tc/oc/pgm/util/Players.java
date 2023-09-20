package tc.oc.pgm.util;

import cloud.commandframework.context.CommandContext;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.util.CommandKeys;

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
    String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
    List<String> playerSuggestions =
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> Players.isVisible(sender, p))
            .map(p -> Players.getVisibleName(sender, p))
            .filter(n -> LiquidMetal.match(n, query))
            .collect(Collectors.toList());
    List<String> prefixMatched =
        playerSuggestions.stream()
            .filter((suggestion) -> suggestion.toLowerCase(Locale.ROOT).startsWith(lowerCaseQuery))
            .collect(Collectors.toList());
    if (!prefixMatched.isEmpty()) {
      return prefixMatched;
    } else {
      return playerSuggestions;
    }
  }

  public static Player getPlayer(CommandSender sender, String query) {
    return StringUtils.bestFuzzyMatch(
        query,
        Bukkit.getOnlinePlayers().stream().filter(p -> Players.isVisible(sender, p)).iterator(),
        p -> Players.getVisibleName(sender, p));
  }

  public static MatchPlayer getMatchPlayer(CommandSender sender, String query) {
    return PGM.get().getMatchManager().getPlayer(getPlayer(sender, query));
  }

  public static boolean isFriend(CommandSender viewer, Player other) {
    return viewer instanceof Player && Integration.isFriend((Player) viewer, other);
  }

  public static List<String> suggestPlayers(CommandContext<CommandSender> context, String input) {
    List<String> queue = context.get(CommandKeys.INPUT_QUEUE);

    return getPlayerNames(context.getSender(), queue.isEmpty() ? "" : queue.get(queue.size() - 1));
  }
}
