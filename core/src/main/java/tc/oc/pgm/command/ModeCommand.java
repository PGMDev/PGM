package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.clock;
import static tc.oc.pgm.util.text.TextException.exception;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Default;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.WordUtils;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ModesPaginatedResult;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.pgm.util.Audience;

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
        TextComponent.Builder builder =
            text().append(text("Next Mode: ", NamedTextColor.DARK_PURPLE));

        ModeChangeCountdown next = countdowns.get(0);
        Duration timeLeft = modes.getCountdown().getTimeLeft(next);

        if (timeLeft == null) {
          builder.append(text(next.getMode().getPreformattedMaterialName(), NamedTextColor.GOLD));
        } else if (timeLeft.getSeconds() >= 0) {
          builder.append(
              text(
                      WordUtils.capitalize(next.getMode().getPreformattedMaterialName()),
                      NamedTextColor.GOLD)
                  .append(space())
                  .append(text("(", NamedTextColor.AQUA))
                  .append(
                      new ModesPaginatedResult(modes)
                          .formatSingleCountdown(next)
                          .color(NamedTextColor.AQUA))
                  .append(text(")", NamedTextColor.AQUA)));
        } else {
          throwNoResults();
        }

        audience.sendMessage(builder.build());
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

    TextComponent.Builder builder =
        text().append(text("All modes have been pushed ", NamedTextColor.GOLD));
    if (duration.isNegative()) {
      builder.append(text("backwards ", NamedTextColor.GOLD));
    } else {
      builder.append(text("forwards ", NamedTextColor.GOLD));
    }

    builder.append(text("by ", NamedTextColor.GOLD));
    builder.append(clock(Math.abs(duration.getSeconds())).color(NamedTextColor.AQUA));

    audience.sendMessage(builder);
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
    throw exception("command.noResults");
  }
}
