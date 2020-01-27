package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import tc.oc.component.render.ComponentRenderers;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.result.TieVictoryCondition;
import tc.oc.pgm.result.VictoryCondition;
import tc.oc.pgm.result.VictoryConditions;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.util.components.PeriodFormats;

@CommandContainer
public class TimeLimitCommands {

  @Command(
          name = "timelimit",
          aliases = {"tl"},
          desc = "Start, update, or cancel a time limit. Result can be 'default', 'objectives', 'tie', or the name of a team",
          descFooter = "cancel | [-r result] [duration]")
  public static void timelimit(
      CommandSender sender,
      Match match,
      String durationString,
      @ArgFlag(name = 'r', desc = "Result")String resultString)
  {
    TimeLimitMatchModule tlmm = match.getMatchModule(TimeLimitMatchModule.class);
    TimeLimit existing = tlmm.getTimeLimit();

    if (resultString == null && durationString == null) {
      if (existing != null) {
        if (match.isFinished() && tlmm.getFinalRemaining() != null) {
          sender.sendMessage(
              ChatColor.YELLOW
                  + "The match ended with "
                  + ChatColor.AQUA
                  + PeriodFormats.COLONS_PRECISE.print(tlmm.getFinalRemaining().toPeriod())
                  + ChatColor.YELLOW
                  + " remaining");
        } else {
          sendTimeLimitMessage(sender, match, existing.getDuration(), existing.getResult());
        }
      } else {
        sender.sendMessage(ChatColor.YELLOW + "There is no time limit");
      }
    } else {
      if (!sender.hasPermission(Permissions.GAMEPLAY)) {
        throw new SecurityException("You don't have permission to do that");
      }

      if ("cancel".equals(durationString)) {
        tlmm.cancel();
        tlmm.setTimeLimit(null);
        sender.sendMessage(ChatColor.YELLOW + "Time limit cancelled");
      } else {
        VictoryCondition result;
        if (resultString != null) {
          try {
            result = VictoryConditions.parse(match.getMapContext(), resultString);
          } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid result or team name: " + resultString);
          }
        } else if (existing != null) {
          result = existing.getResult();
        } else {
          result = null;
        }

        Duration duration;
        if (durationString != null) {
          try {
            duration = PeriodFormats.SHORTHAND.parsePeriod(durationString).toStandardDuration();
          } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid time format: " + durationString);
          }
        } else if (existing != null) {
          duration = existing.getDuration();
        } else {
          throw new IllegalArgumentException("Please specify a duration");
        }

        tlmm.cancel();
        tlmm.setTimeLimit(new TimeLimit(null, duration, result, true));
        tlmm.start();

        sendTimeLimitMessage(sender, match, duration, result);
      }
    }
  }

  private static void sendTimeLimitMessage(
      CommandSender sender, Match match, Duration duration, VictoryCondition result) {
    if (result == null) {
      for (VictoryCondition condition : match.getVictoryConditions()) {
        if (!(condition instanceof TimeLimit)) {
          result = condition;
          break;
        }
      }

      if (result == null) {
        result = new TieVictoryCondition();
      }
    }

    ComponentRenderers.send(
        sender,
        new PersonalizedText(net.md_5.bungee.api.ChatColor.YELLOW)
            .extra(
                new PersonalizedTranslatable(
                    "timeLimit.commandOutput",
                    new PersonalizedText(
                        PeriodFormats.COLONS_PRECISE.print(duration.toPeriod()),
                        net.md_5.bungee.api.ChatColor.AQUA),
                    result.getDescription(match))));
  }
}
