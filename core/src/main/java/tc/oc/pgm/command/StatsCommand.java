package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.util.text.TextFormatter;

public final class StatsCommand {

  @Command(
      aliases = {"stats"},
      desc = "Show your stats for the match")
  public void stats(Audience audience, CommandSender sender, MatchPlayer player, Match match) {
    if (match.isFinished()
        && PGM.get().getConfiguration().showVerboseStats()
        && !match.getCompetitors().stream()
            .allMatch(c -> c instanceof Tribute)) { // Should not try to trigger on FFA
      match.needModule(StatsMatchModule.class).giveVerboseStatsItem(player, true);
    } else if (player.getSettings().getValue(SettingKey.STATS).equals(SettingValue.STATS_ON)) {
      audience.sendMessage(
          TextFormatter.horizontalLineHeading(
              sender,
              translatable("match.stats.you", NamedTextColor.DARK_GREEN),
              NamedTextColor.WHITE));
      audience.sendMessage(
          match.needModule(StatsMatchModule.class).getBasicStatsMessage(player.getId()));
    } else {
      throw exception("match.stats.disabled");
    }
  }
}
