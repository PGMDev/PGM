package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
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
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.countdowns.CountdownContext;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ModesPaginatedResult;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;

// TODO: make the output nicer
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
            text()
                .append(
                    translatable("command.nextMode", NamedTextColor.DARK_PURPLE).append(space()));

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
      desc = "List all objective modes",
      usage = "[page]")
  public void list(Audience audience, Match match, @Default("1") int page) throws CommandException {
    showList(page, audience, getModes(match));
  }

  @Command(
      aliases = {"push"},
      desc = "Reschedule all objective modes with active countdowns",
      perms = Permissions.GAMEPLAY)
  public void push(Audience audience, Match match, Duration duration) {
    ObjectiveModesMatchModule modes = getModes(match);

    if (!match.isRunning()) {
      throwMatchNotStarted();
    }

    if (modes == null) {
      throwNoResults();
    }

    CountdownContext countdowns = modes.getCountdown();
    List<ModeChangeCountdown> sortedCountdowns = modes.getSortedCountdowns(false);

    for (ModeChangeCountdown countdown : sortedCountdowns) {
      if (countdowns.getTimeLeft(countdown).getSeconds() > 0
          && countdown.getMode().getFilter() == null) {
        Duration oldDelay = countdowns.getTimeLeft(countdown);
        Duration newDelay = oldDelay.plus(duration);

        countdowns.cancel(countdown);
        countdowns.start(countdown, (int) newDelay.getSeconds());
      }
    }

    TextComponent.Builder builder =
        text().append(translatable("command.modesPushed", NamedTextColor.GOLD).append(space()));
    if (duration.isNegative()) {
      builder.append(translatable("command.modesPushedBack", NamedTextColor.GOLD).append(space()));
    } else {
      builder.append(
          translatable("command.modesPushedForwards", NamedTextColor.GOLD).append(space()));
    }

    builder.append(translatable("command.modesPushedBy", NamedTextColor.GOLD).append(space()));
    builder.append(clock(Math.abs(duration.getSeconds())).color(NamedTextColor.AQUA));

    audience.sendMessage(builder);
  }

  @Command(
      aliases = {"start"},
      desc = "Starts an objective mode",
      perms = Permissions.GAMEPLAY)
  public void start(Audience audience, Match match, int modeNumber, Duration duration)
      throws CommandException {
    ObjectiveModesMatchModule modes = getModes(match);

    if (!match.isRunning()) {
      throwMatchNotStarted();
    }
    if (modes == null) {
      throwNoResults();
    }
    modeNumber--;
    CountdownContext countdowns = modes.getCountdown();
    List<ModeChangeCountdown> sortedCountdowns = modes.getSortedCountdowns(true);
    ModeChangeCountdown selectedMode;

    if (sortedCountdowns.toArray().length < modeNumber) {
      throwInvalidNumber(Integer.toString(modeNumber));
    }
    if (duration.isNegative()) {
      throwInvalidNumber(duration.toString());
    }
    selectedMode = sortedCountdowns.get(modeNumber);
    countdowns.cancel(selectedMode);
    countdowns.start(selectedMode, duration);

    String modeName;
    if (selectedMode.getMode().getName() != null) {
      modeName = selectedMode.getMode().getName();
    } else {
      modeName = selectedMode.getMode().getPreformattedMaterialName();
    }

    TextComponent.Builder builder =
        text()
            .append(
                translatable("command.selectedModePushed", text(modeName))
                    .color(NamedTextColor.GOLD)
                    .append(space()));
    builder.append(clock(Math.abs(duration.getSeconds())).color(NamedTextColor.AQUA));
    audience.sendMessage(builder);
  }

  private static void showList(int page, Audience audience, ObjectiveModesMatchModule modes)
      throws CommandException {
    if (modes == null) {
      throwNoResults();
    } else {
      new ModesPaginatedResult(modes).display(audience, modes.getSortedCountdowns(true), page);
    }
  }

  private static ObjectiveModesMatchModule getModes(Match match) {
    return match.getModule(ObjectiveModesMatchModule.class);
  }

  private static void throwNoResults() {
    throw exception("command.emptyResult");
  }

  private static void throwMatchNotStarted() {
    throw exception("command.matchNotStarted");
  }

  private static void throwInvalidNumber(String invalidNumber) {
    throw exception("command.invalidNumber", text(invalidNumber));
  }
}
