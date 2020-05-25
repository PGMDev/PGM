package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.modules.StatsMatchModule;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.component.ComponentUtils;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

public final class StatsCommand {

  @Command(
      aliases = {"stats"},
      desc = "Show your stats for the match")
  public void stats(Audience audience, CommandSender sender, MatchPlayer player, Match match) {
    if (player.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      audience.sendMessage(
          ComponentUtils.horizontalLineHeading(
              ChatColor.DARK_GREEN + TextTranslations.translate("match.stats.you", sender),
              ChatColor.WHITE,
              ComponentUtils.MAX_CHAT_WIDTH));
      audience.sendMessage(
          match.needModule(StatsMatchModule.class).getBasicStatsMessage(player.getId()));
    } else {
      throw TextException.of("match.stats.disabled");
    }
  }
}
