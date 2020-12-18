package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import java.time.Duration;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.chat.Audience;

public final class TimeLimitCommand {

  @Command(
      aliases = {"timelimit", "tl"},
      desc = "Start a time limit",
      usage = "duration [result] [overtime] [max-overtime]",
      help = "Result can be 'default', 'objectives', 'tie', or the name of a team",
      flags = "r",
      perms = Permissions.GAMEPLAY)
  public void timelimit(
      Audience audience,
      Match match,
      Duration duration,
      @Nullable VictoryCondition result,
      @Nullable Duration overtime,
      @Nullable Duration maxOvertime) {

    final TimeLimitMatchModule time = match.needModule(TimeLimitMatchModule.class);

    time.cancel();
    time.setTimeLimit(
        new TimeLimit(
            null,
            duration.isNegative() ? Duration.ZERO : duration,
            overtime,
            maxOvertime,
            result,
            true));
    time.start();

    audience.sendMessage(
        Component.translatable(
            "match.timeLimit.commandOutput",
            NamedTextColor.YELLOW,
            Component.text(TimeUtils.formatDuration(duration), NamedTextColor.AQUA),
            result != null
                ? result.getDescription(match)
                : Component.translatable("misc.unknown")));
  }
}
