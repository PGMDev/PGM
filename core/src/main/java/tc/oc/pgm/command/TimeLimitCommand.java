package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import java.time.Duration;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.util.Audience;

public final class TimeLimitCommand {

  @CommandMethod("timelimit|tl <duration> [result] [overtime] [max-overtime] [end-overtime]")
  @CommandDescription(
      "Start a time limit. Result can be 'default', 'objectives', 'tie', or the name of a team")
  @CommandPermission(Permissions.GAMEPLAY)
  public void timelimit(
      Audience audience,
      Match match,
      TimeLimitMatchModule time,
      @Argument("duration") Duration duration,
      @Argument(value = "result", defaultValue = CURRENT) Optional<VictoryCondition> result,
      @Argument("overtime") Duration overtime,
      @Argument("max-overtime") Duration maxOvertime,
      @Argument("end-overtime") Duration endOvertime) {
    time.cancel();
    time.setTimeLimit(
        new TimeLimit(
            null,
            duration.isNegative() ? Duration.ZERO : duration,
            overtime,
            maxOvertime,
            endOvertime,
            result.orElse(null),
            true));
    time.start();

    audience.sendMessage(
        translatable(
            "match.timeLimit.commandOutput",
            NamedTextColor.YELLOW,
            clock(duration).color(NamedTextColor.AQUA),
            result.map(r -> r.getDescription(match)).orElse(translatable("misc.unknown"))));
  }
}
