package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.modules.StatsMatchModule;
import tc.oc.util.bukkit.component.ComponentUtils;
import tc.oc.util.bukkit.translations.AllTranslations;

public class StatCommands {

  @Command(
      aliases = {"stats"},
      desc = "Shows your stats for this match")
  public static void checkStats(CommandSender sender, MatchPlayer player, Match match) {

    sender.sendMessage(
        ComponentUtils.horizontalLineHeading(
            ChatColor.DARK_GREEN + AllTranslations.get().translate("stats.current", sender),
            ChatColor.WHITE,
            ComponentUtils.MAX_CHAT_WIDTH));
    sender.sendMessage(match.getModule(StatsMatchModule.class).getBasicStatsMessage(player.getId()));
  }
}
