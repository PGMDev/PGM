package tc.oc.pgm.command;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ModesPaginatedResult;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.TextException;

// TODO: make the output nicer and translate
public final class ModeCommand {

  @Command(
      aliases = {"next"},
      desc = "Show the next objective mode")
  public void next(Audience audience, Match match) {
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
        } else if (timeLeft.getSeconds() >= 0) {
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

        audience.sendMessage(builder.toString());
      }
    }
  }

  @Command(
      aliases = {"list", "page"},
      desc = "List all objectivs modes",
      usage = "[page]")
  public void list(Audience audience, Match match, @Default("1") int page) throws CommandException {
    showList(page, audience, getModes(match));
  }

  @Command(
      aliases = {"push"},
      desc = "Reschedule an objective mode",
      perms = Permissions.GAMEPLAY)
  public void push(Audience audience, Match match, Duration duration) {
    ObjectiveModesMatchModule modes = getModes(match);

    if (modes == null) {
      throwNoResults();
    }

    CountdownContext countdowns = modes.getCountdown();
    List<ModeChangeCountdown> sortedCountdowns = modes.getSortedCountdowns();

    for (ModeChangeCountdown countdown : sortedCountdowns) {
      if (countdowns.getTimeLeft(countdown).getSeconds() > 0) {
        Duration oldDelay = countdowns.getTimeLeft(countdown);
        Duration newDelay = oldDelay.plus(duration);

        countdowns.cancel(countdown);
        countdowns.start(countdown, (int) newDelay.getSeconds());
      }
    }

    StringBuilder builder = new StringBuilder(ChatColor.GOLD + "All modes have been pushed ");
    if (duration.isNegative()) {
      builder.append("backwards ");
    } else {
      builder.append("forwards ");
    }

    builder.append("by ").append(TimeUtils.formatDuration(duration));

    audience.sendMessage(builder.toString());
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

  private static void throwNoResults() {
    throw TextException.of("command.noResults");
  }
}
