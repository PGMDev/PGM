package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.bukkit.parametric.Type;
import app.ashcon.intake.bukkit.parametric.annotation.Fallback;
import app.ashcon.intake.parametric.annotation.Switch;
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

public class TimeLimitCommands {

  @Command(
      aliases = {"timelimit", "tl"},
      desc = "Start, update, or cancel a time limit",
      usage = "cancel | [-r result] [duration]",
      help = "Result can be 'default', 'objectives', 'tie', or the name of a team",
      flags = "r")
  public static void timelimit(
      CommandSender sender,
      Match match,
      String durationString,
      @Fallback(Type.NULL) @Switch('r') String resultString)
      throws CommandException {
    TimeLimitMatchModule tlmm = match.getModule(TimeLimitMatchModule.class);
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
        throw new CommandException("You don't have permission to do that");
      }

      if ("cancel".equals(durationString)) {
        tlmm.cancel();
        tlmm.setTimeLimit(null);
        sender.sendMessage(ChatColor.YELLOW + "Time limit cancelled");
      } else {
        VictoryCondition result;
        if (resultString != null) {
          try {
            result = VictoryConditions.parse(match, resultString);
          } catch (IllegalArgumentException ex) {
            throw new CommandException("Invalid result or team name: " + resultString);
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
            throw new CommandException("Invalid time format: " + durationString);
          }
        } else if (existing != null) {
          duration = existing.getDuration();
        } else {
          throw new CommandException("Please specify a duration");
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
