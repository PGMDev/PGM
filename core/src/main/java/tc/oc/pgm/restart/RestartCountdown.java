package tc.oc.pgm.restart;

import java.time.Duration;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.TimeUtils;

public class RestartCountdown extends MatchCountdown {

  public RestartCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    if (TimeUtils.isLongerThan(remaining, Duration.ZERO)) {
      return TranslatableComponent.of(
          "countdown.restart", TextColor.AQUA, secondsRemaining(TextColor.DARK_RED));
    } else {
      return TranslatableComponent.of("misc.serverRestart", TextColor.RED);
    }
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
  }

  @Override
  public void onEnd(Duration total) {
    super.onEnd(total);
    Bukkit.getServer().shutdown();
  }
}
