package tc.oc.pgm.util;

import com.google.common.base.Preconditions;
import org.bukkit.ChatColor;
import org.joda.time.Duration;
import org.joda.time.Period;
import tc.oc.pgm.modes.ModeChangeCountdown;
import tc.oc.pgm.modes.ObjectiveModesMatchModule;
import tc.oc.util.bukkit.component.Components;
import tc.oc.util.bukkit.component.PeriodFormats;
import tc.oc.util.bukkit.component.types.PersonalizedText;

/** Class used to display a paginated list of monument modes */
public class ModesPaginatedResult extends PrettyPaginatedResult<ModeChangeCountdown> {

  private final ObjectiveModesMatchModule modes;

  public ModesPaginatedResult(ObjectiveModesMatchModule modes) {
    super("Monument Modes");
    this.modes = Preconditions.checkNotNull(modes);
  }

  @Override
  public String format(ModeChangeCountdown countdown, int index) {
    String materialName = countdown.getMode().getPreformattedMaterialName();
    Period timeFromStart = countdown.getMode().getAfter().toPeriod();

    StringBuilder builder = new StringBuilder();

    builder.append(ChatColor.GOLD).append(index + 1).append(". ");
    builder.append(ChatColor.LIGHT_PURPLE).append(materialName).append(" - ");
    builder.append(ChatColor.AQUA).append(PeriodFormats.COLONS.print(timeFromStart));

    if (countdown.getMatch().isRunning()) {
      builder
          .append(ChatColor.DARK_AQUA)
          .append(" (")
          .append(this.formatSingleCountdown(countdown))
          .append(')');
    }

    if (this.isExpired(countdown)) {
      return Components.strikethrough(new PersonalizedText(builder.toString()), true)
          .toLegacyText();
    } else {
      return builder.toString();
    }
  }

  /**
   * Formats a {@link ModeChangeCountdown} to the following format 'm:ss' and appends 'left' to the
   * text
   *
   * @param countdown to format
   * @return Formatted text
   */
  public String formatSingleCountdown(ModeChangeCountdown countdown) {
    Duration currentTimeLeft = modes.getCountdown().getTimeLeft(countdown);
    StringBuilder builder = new StringBuilder();

    if (countdown.getMatch().isRunning()) {
      if (this.isRunning(countdown)) {
        builder.append(PeriodFormats.COLONS.print(currentTimeLeft.toPeriod())).append(" left");
      } else {
        builder.append("0:00 left");
      }
    }

    return builder.toString();
  }

  private boolean isRunning(ModeChangeCountdown countdown) {
    Duration timeLeft = this.modes.getCountdown().getTimeLeft(countdown);
    return timeLeft != null && timeLeft.getStandardSeconds() > 0;
  }

  private boolean isExpired(ModeChangeCountdown countdown) {
    Duration timeLeft = this.modes.getCountdown().getTimeLeft(countdown);
    return timeLeft != null && timeLeft.getStandardSeconds() <= 0;
  }
}
