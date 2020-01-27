package tc.oc.pgm.commands;

import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.Command;
import java.util.List;
import java.util.NoSuchElementException;

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

@CommandContainer
public class ModeCommands {

  @Command(
          name = "next",
          desc = "Shows information about the next mode")
  public static void next(CommandSender sender, Match match) {
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
      name = "page",
      aliases = {"list"},
      desc = "Lists all modes",
      descFooter = "[page]")
  public static void list(Audience audience, Match match, /*@Default("1")TODO ADD DEFAULT*/ int page)
  {
    showList(page, audience, getModes(match));
  }

  @Command(
      name = "push",
      desc = "Reschedules all active mode change countdowns by [seconds]",
      descFooter = "[seconds]")
  public static void push(CommandSender sender, Match match, Duration seconds)
  {
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
      throw new IllegalArgumentException("Invalid time format specified");
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
  {
    if (modes == null) {
      throwNoResults();
    } else {
      new ModesPaginatedResult(modes).display(audience, modes.getSortedCountdowns(), page);
    }
  }

  private static ObjectiveModesMatchModule getModes(Match match) {
    return match.getMatchModule(ObjectiveModesMatchModule.class);
  }

  private static void throwNoResults() {
    throw new NoSuchElementException("No results match!");
  }
}
