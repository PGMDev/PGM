package tc.oc.pgm.modes;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.clock;

import com.google.common.base.Preconditions;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.util.PrettyPaginatedResult;

/** Class used to display a paginated list of monument modes */
public class ModesPaginatedResult extends PrettyPaginatedResult<ModeChangeCountdown> {

  private final ObjectiveModesMatchModule modes;

  public ModesPaginatedResult(ObjectiveModesMatchModule modes) {
    // TODO translate this
    super("Monument Modes");
    this.modes = Preconditions.checkNotNull(modes);
  }

  @Override
  public Component format(ModeChangeCountdown countdown, int index) {
    String materialName = countdown.getMode().getPreformattedMaterialName();
    Duration timeFromStart = countdown.getMode().getAfter();
    Duration remainingTime = countdown.getRemaining();
    boolean isRunning = countdown.getMatch().isRunning();

    TextComponent.Builder builder = text();

    builder.append(text((index + 1) + ". ", NamedTextColor.GOLD));
    builder.append(text(materialName + " - ", NamedTextColor.LIGHT_PURPLE));
    builder.append(clock(timeFromStart).color(NamedTextColor.AQUA));
    if (!isRunning && countdown.getMode().getFilter() != null) {
      builder.append(space()).append(translatable("misc.crossmark", NamedTextColor.DARK_RED));
    }

    if (isRunning && remainingTime != null) {
      if (countdown.getMode().getFilter() != null) {
        builder.append(space()).append(translatable("misc.checkmark", NamedTextColor.GREEN));
      }
      builder.append(text(" (", NamedTextColor.DARK_AQUA));
      builder.append(this.formatSingleCountdown(countdown).color(NamedTextColor.DARK_AQUA));
      builder.append(text(")", NamedTextColor.DARK_AQUA));
    } else if (isRunning) {
      builder.append(space()).append(translatable("misc.crossmark", NamedTextColor.DARK_RED));
      builder.append(text(" (", NamedTextColor.DARK_AQUA));
      builder.append(translatable("command.conditionUnmet", NamedTextColor.DARK_AQUA));
      builder.append(text(")", NamedTextColor.DARK_AQUA));
    }

    return builder.decoration(TextDecoration.STRIKETHROUGH, this.isExpired(countdown)).build();
  }

  /**
   * Formats a {@link tc.oc.pgm.modes.ModeChangeCountdown} to the following format 'm:ss' and
   * appends 'left' to the text //TODO make translatable
   *
   * @param countdown to format
   * @return Formatted text
   */
  public TextComponent.Builder formatSingleCountdown(ModeChangeCountdown countdown) {
    Duration currentTimeLeft = modes.getCountdown().getTimeLeft(countdown);

    if (countdown.getMatch().isRunning() && currentTimeLeft != null) {
      return clock(currentTimeLeft).append(text(" left"));
    }

    return text();
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
