package tc.oc.pgm.modes;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;

/** Class used to display a paginated list of monument modes */
public class ModesPaginatedResult extends PrettyPaginatedComponentResults<ModeChangeCountdown> {

  private final ObjectiveModesMatchModule modes;
  public static final TextComponent SYMBOL_INCOMPLETE =
      text("\u2715", NamedTextColor.DARK_RED); // ✕
  public static final TextComponent SYMBOL_COMPLETE = text("\u2714", NamedTextColor.GREEN); // ✔

  public ModesPaginatedResult(ObjectiveModesMatchModule modes) {
    super(null);
    this.modes = modes;
  }

  public ModesPaginatedResult(
      Component header, int resultsPerPage, ObjectiveModesMatchModule modes) {
    super(header, resultsPerPage);
    this.modes = assertNotNull(modes);
  }

  @Override
  public Component format(ModeChangeCountdown countdown, int index) {
    String name = countdown.getMode().getLegacyName();
    Duration timeFromStart = countdown.getMode().getAfter();
    Duration remainingTime = countdown.getRemaining();
    boolean isRunning = countdown.getMatch().isRunning();

    TextComponent.Builder builder = text();

    builder.append(text((index + 1) + ". ", NamedTextColor.GOLD));
    builder.append(text(name, NamedTextColor.LIGHT_PURPLE));
    builder.append(space());
    builder.append(clock(timeFromStart).color(NamedTextColor.AQUA));
    if (!isRunning && countdown.getMode().getFilter() != null) {
      builder.append(space()).append(SYMBOL_INCOMPLETE);
    }

    if (isRunning && remainingTime != null) {
      if (countdown.getMode().getFilter() != null) {
        builder.append(space()).append(SYMBOL_COMPLETE);
      }
      builder.append(text(" (", NamedTextColor.DARK_AQUA));
      builder.append(this.formatSingleCountdown(countdown).color(NamedTextColor.DARK_AQUA));
      builder.append(text(")", NamedTextColor.DARK_AQUA));
    } else if (isRunning) {
      builder.append(space()).append(SYMBOL_INCOMPLETE);
      builder.append(text(" (", NamedTextColor.DARK_AQUA));
      builder.append(translatable("command.conditionUnmet", NamedTextColor.DARK_AQUA));
      builder.append(text(")", NamedTextColor.DARK_AQUA));
    }

    return builder.decoration(TextDecoration.STRIKETHROUGH, this.isExpired(countdown)).build();
  }

  /**
   * Formats a {@link tc.oc.pgm.modes.ModeChangeCountdown} to the following format 'm:ss' and
   * appends 'left' to the text
   *
   * @param countdown to format
   * @return Formatted text
   */
  public TextComponent formatSingleCountdown(ModeChangeCountdown countdown) {
    Duration currentTimeLeft = modes.getCountdown().getTimeLeft(countdown);

    if (countdown.getMatch().isRunning() && currentTimeLeft != null) {
      return clock(currentTimeLeft).append(space().append(translatable("command.timeLeft")));
    }

    return empty();
  }

  private boolean isRunning(ModeChangeCountdown countdown) {
    Duration timeLeft = this.modes.getCountdown().getTimeLeft(countdown);
    return timeLeft != null && timeLeft.getSeconds() > 0;
  }

  private boolean isExpired(ModeChangeCountdown countdown) {
    Duration timeLeft = this.modes.getCountdown().getTimeLeft(countdown);
    return timeLeft != null && timeLeft.getSeconds() <= 0;
  }
}
