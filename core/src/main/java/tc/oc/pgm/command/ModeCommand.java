package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.clock;
import static tc.oc.pgm.util.text.TextException.exception;

import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.WordUtils;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.CountdownContext;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ModesPaginatedResult;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TextFormatter;

@Command("mode|modes")
public final class ModeCommand {

  @Command("next")
  @CommandDescription("Show the next objective mode")
  public void next(Audience audience, ObjectiveModesMatchModule modes) {
    List<ModeChangeCountdown> countdowns = modes.getActiveCountdowns();

    if (countdowns.isEmpty()) throwNoResults();

    TextComponent.Builder builder = text()
        .append(translatable("command.nextMode", NamedTextColor.DARK_PURPLE).append(space()));

    ModeChangeCountdown next = countdowns.get(0);
    Duration timeLeft = modes.getCountdown().getTimeLeft(next);

    if (timeLeft == null) {
      builder.append(text(next.getMode().getLegacyName(), NamedTextColor.GOLD));
    } else if (timeLeft.getSeconds() >= 0) {
      builder.append(text(WordUtils.capitalize(next.getMode().getLegacyName()), NamedTextColor.GOLD)
          .append(space())
          .append(text("(", NamedTextColor.AQUA))
          .append(new ModesPaginatedResult(modes)
              .formatSingleCountdown(next)
              .color(NamedTextColor.AQUA))
          .append(text(")", NamedTextColor.AQUA)));
    } else {
      throwNoResults();
    }

    audience.sendMessage(builder.build());
  }

  @Command("[page]")
  @CommandDescription("List all objective modes")
  public void fallback(
      Audience audience,
      ObjectiveModesMatchModule modes,
      @Argument("page") @Default("1") @Range(min = "1") int page) {
    list(audience, modes, page);
  }

  @Command("list|page [page]")
  @CommandDescription("List all objective modes")
  public void list(
      Audience audience,
      ObjectiveModesMatchModule modes,
      @Argument("page") @Default("1") @Range(min = "1") int page) {
    List<ModeChangeCountdown> modeList = modes.getSortedCountdowns(true);
    int resultsPerPage = 8;
    int pages = (modeList.size() + resultsPerPage - 1) / resultsPerPage;
    Component header = TextFormatter.paginate(
        translatable("command.monumentModes"),
        page,
        pages,
        NamedTextColor.DARK_AQUA,
        NamedTextColor.AQUA,
        true);

    new ModesPaginatedResult(header, resultsPerPage, modes).display(audience, modeList, page);
  }

  @Command("push <time>")
  @CommandDescription("Reschedule all objective modes with active countdowns")
  @Permission(Permissions.GAMEPLAY)
  public void push(
      Audience audience,
      Match match,
      ObjectiveModesMatchModule modes,
      @Argument("time") Duration duration) {
    if (!match.isRunning()) throwMatchNotStarted();

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
      builder.append(
          translatable("command.modesPushedBack", NamedTextColor.GOLD).append(space()));
    } else {
      builder.append(
          translatable("command.modesPushedForwards", NamedTextColor.GOLD).append(space()));
    }

    builder.append(translatable("command.modesPushedBy", NamedTextColor.GOLD).append(space()));
    builder.append(clock(Math.abs(duration.getSeconds())).color(NamedTextColor.AQUA));

    audience.sendMessage(builder);
  }

  @Command("start <mode> [time]")
  @CommandDescription("Starts an objective mode")
  @Permission(Permissions.GAMEPLAY)
  public void start(
      Audience audience,
      Match match,
      ObjectiveModesMatchModule modes,
      @Argument("mode") Mode mode,
      @Argument("time") @Default("0s") Duration time) {
    if (!match.isRunning()) throwMatchNotStarted();
    if (time.isNegative()) throwInvalidNumber(time.toString());

    CountdownContext context = modes.getCountdown();
    ModeChangeCountdown countdown = modes.getCountdown(mode);

    context.cancel(countdown);
    context.start(countdown, time);

    TextComponent.Builder builder = text()
        .append(translatable("command.selectedModePushed", mode.getComponentName())
            .color(NamedTextColor.GOLD)
            .append(space()));
    builder.append(clock(Math.abs(time.getSeconds())).color(NamedTextColor.AQUA));
    audience.sendMessage(builder);
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
