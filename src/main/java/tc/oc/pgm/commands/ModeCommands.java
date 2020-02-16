package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import java.util.List;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.joda.time.Duration;
import org.joda.time.Period;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.pgm.util.ModesPaginatedResult;
import tc.oc.util.components.PeriodFormats;

public class ModeCommands {

  @Command(aliases = "next", desc = "Shows information about the next mode")
  public static void next(CommandSender sender, Match match) throws CommandException {
    ObjectiveModesMatchModule modes = getModes(match);

    if (modes == null) {
      throwNoResults();
    } else {
      List<ModeChangeCountdown> countdowns = modes.getActiveCountdowns();

      if (countdowns.isEmpty()) {
        throwNoResults();
      } else {
        StringBuilder builder = new StringBuilder(ChatColor.DARK_PURPLE + "Next mode: ");

        ModeChangeCountdown next = countdowns.get(0);
        Duration timeLeft = modes.getCountdown().getTimeLeft(next);

        if (timeLeft == null) {
          builder.append(ChatColor.GOLD).append(next.getMode().getPreformattedMaterialName());
        } else if (timeLeft.getStandardSeconds() >= 0) {
          builder
              .append(ChatColor.GOLD)
              .append(WordUtils.capitalize(next.getMode().getPreformattedMaterialName()))
              .append(" ")
              .append(ChatColor.AQUA)
              .append("(")
              .append(new ModesPaginatedResult(modes).formatSingleCountdown(next))
              .append(")");
        } else {
          throwNoResults();
        }

        sender.sendMessage(builder.toString());
      }
    }
  }

  @Command(
      aliases = {"list", "page"},
      desc = "Lists all modes",
      usage = "[page]")
  public static void list(Audience audience, Match match, @Default("1") int page)
      throws CommandException {
    showList(page, audience, getModes(match));
  }

  @Command(
      aliases = {"push"},
      desc = "Reschedules all active mode change countdowns by [seconds]",
      usage = "[seconds]",
      perms = Permissions.GAMEPLAY)
  public static void push(CommandSender sender, Match match, Duration seconds)
      throws CommandException {
    ObjectiveModesMatchModule modes = getModes(match);

    if (modes == null) {
      throwNoResults();
    }

    CountdownContext countdowns = modes.getCountdown();
    List<ModeChangeCountdown> sortedCountdowns = modes.getSortedCountdowns();

    Period offset;
    try {
      offset = seconds.toPeriod();
    } catch (IllegalArgumentException e) {
      throw new CommandException("Invalid time format specified");
    }

    for (ModeChangeCountdown countdown : sortedCountdowns) {
      if (countdowns.getTimeLeft(countdown).getStandardSeconds() > 0) {
        Duration oldDelay = countdowns.getTimeLeft(countdown);
        Duration newDelay = oldDelay.plus(offset.toStandardDuration());

        countdowns.cancel(countdown);
        countdowns.start(countdown, (int) newDelay.getStandardSeconds());
      }
    }

    StringBuilder builder = new StringBuilder(ChatColor.GOLD + "All modes have been pushed ");
    if (offset.toStandardDuration().getStandardSeconds() >= 0) {
      builder.append("forwards ");
    } else {
      builder.append("backwards ");
    }

    builder.append("by ").append(PeriodFormats.COLONS.print(offset));

    sender.sendMessage(builder.toString());
  }

  private static void showList(int page, Audience audience, ObjectiveModesMatchModule modes)
      throws CommandException {
    if (modes == null) {
      throwNoResults();
    } else {
      new ModesPaginatedResult(modes).display(audience, modes.getSortedCountdowns(), page);
    }
  }

  private static ObjectiveModesMatchModule getModes(Match match) {
    return match.getModule(ObjectiveModesMatchModule.class);
  }

  private static void throwNoResults() throws CommandException {
    throw new CommandException("No results match!");
  }
}
