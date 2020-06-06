package tc.oc.pgm.modes;

import com.google.common.base.Preconditions;
import java.time.Duration;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextDecoration;
import org.bukkit.ChatColor;
import tc.oc.pgm.util.PrettyPaginatedResult;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.text.TextTranslations;

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
    Duration timeFromStart = countdown.getMode().getAfter();

    StringBuilder builder = new StringBuilder();

    builder.append(ChatColor.GOLD).append(index + 1).append(". ");
    builder.append(ChatColor.LIGHT_PURPLE).append(materialName).append(" - ");
    builder.append(ChatColor.AQUA).append(TimeUtils.formatDuration(timeFromStart));

    if (countdown.getMatch().isRunning()) {
      builder
          .append(ChatColor.DARK_AQUA)
          .append(" (")
          .append(this.formatSingleCountdown(countdown))
          .append(')');
    }

    if (this.isExpired(countdown)) {
      return TextTranslations.translateLegacy(
          TextComponent.of(builder.toString()).decoration(TextDecoration.STRIKETHROUGH, true),
          null);
    } else {
      return builder.toString();
    }
  }

  /**
   * Formats a {@link tc.oc.pgm.modes.ModeChangeCountdown} to the following format 'm:ss' and
   * appends 'left' to the text
   *
   * @param countdown to format
   * @return Formatted text
   */
  public String formatSingleCountdown(ModeChangeCountdown countdown) {
    Duration currentTimeLeft = modes.getCountdown().getTimeLeft(countdown);
    StringBuilder builder = new StringBuilder();

    if (countdown.getMatch().isRunning()) {
      if (this.isRunning(countdown)) {
        builder.append(TimeUtils.formatDuration(currentTimeLeft)).append(" left");
      } else {
        builder.append("0:00 left");
      }
    }

    return builder.toString();
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
