package tc.oc.pgm.restart;

import java.time.Duration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.util.TimeUtils;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedTranslatable;

public class RestartCountdown extends MatchCountdown {

  public RestartCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    if (TimeUtils.isLongerThan(remaining, Duration.ZERO)) {
      return new PersonalizedTranslatable(
              "broadcast.serverRestart.message", secondsRemaining(ChatColor.DARK_RED))
          .color(ChatColor.AQUA);
    } else {
      return new PersonalizedTranslatable("broadcast.serverRestart.kickMsg").color(ChatColor.RED);
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
