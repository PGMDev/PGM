package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.modules.StatsMatchModule;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class StatsCommands {

  @Command(
      aliases = {"stats"},
      desc = "Shows your stats for this match")
  public static void checkStats(CommandSender sender, MatchPlayer player, Match match)
      throws CommandException {
    if (player.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      sender.sendMessage(
          ComponentUtils.horizontalLineHeading(
              ChatColor.DARK_GREEN + TextTranslations.translate("match.stats.you", sender),
              ChatColor.WHITE,
              ComponentUtils.MAX_CHAT_WIDTH));
      sender.sendMessage(
          match.getModule(StatsMatchModule.class).getBasicStatsMessage(player.getId()));
    } else throw new CommandException(TextTranslations.translate("match.stats.disabled", sender));
  }
}
