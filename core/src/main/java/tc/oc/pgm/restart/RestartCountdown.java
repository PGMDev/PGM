package tc.oc.pgm.restart;

import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.util.TimeUtils;

public class RestartCountdown extends MatchCountdown {

  public RestartCountdown(Match match) {
    super(match, BossBar.Color.BLUE);
  }

  @Override
  protected Component formatText() {
    if (TimeUtils.isLongerThan(remaining, Duration.ZERO)) {
      return translatable(
          "countdown.restart", NamedTextColor.AQUA, secondsRemaining(NamedTextColor.DARK_RED));
    } else {
      return translatable("misc.serverRestart", NamedTextColor.RED);
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
