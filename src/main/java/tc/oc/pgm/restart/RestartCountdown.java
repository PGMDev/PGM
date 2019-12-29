package tc.oc.pgm.restart;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.countdowns.MatchCountdown;

public class RestartCountdown extends MatchCountdown {

  public RestartCountdown(Match match) {
    super(match);
  }

  @Override
  protected Component formatText() {
    if (remaining.isLongerThan(Duration.ZERO)) {
      return new PersonalizedText(
          new PersonalizedTranslatable(
              "broadcast.serverRestart.message", secondsRemaining(ChatColor.DARK_RED)),
          ChatColor.AQUA);
    } else {
      return new PersonalizedText(
          new PersonalizedTranslatable("broadcast.serverRestart.kickMsg"), ChatColor.RED);
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
