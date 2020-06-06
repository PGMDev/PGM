package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.modules.StatsMatchModule;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

public final class StatsCommand {

  @Command(
      aliases = {"stats"},
      desc = "Show your stats for the match")
  public void stats(Audience audience, CommandSender sender, MatchPlayer player, Match match) {
    if (player.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      audience.sendMessage(
          TextFormatter.horizontalLineHeading(
              sender,
              TranslatableComponent.of("match.stats.you", TextColor.DARK_GREEN),
              TextColor.WHITE));
      audience.sendMessage(
          match.needModule(StatsMatchModule.class).getBasicStatsMessage(player.getId()));
    } else {
      throw TextException.of("match.stats.disabled");
    }
  }
}
