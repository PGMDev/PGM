package tc.oc.pgm.start;

import java.time.Duration;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.countdowns.MatchCountdown;

/** Common base for countdowns leading up to match start */
public abstract class PreMatchCountdown extends MatchCountdown {

  protected static final Sound COUNT_SOUND =
      Sound.sound(Key.key("note.pling"), Sound.Source.MASTER, 1f, 1.19f);

  public PreMatchCountdown(Match match) {
    super(match);
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);
    getMatch().setPhase(MatchPhase.STARTING);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);
    if (remaining.getSeconds() >= 1 && remaining.getSeconds() <= 3) {
      getMatch().playSound(COUNT_SOUND);
    }
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    getMatch().setPhase(MatchPhase.IDLE);
    getMatch().sendWarning(Component.translatable("broadcast.startCancel"));
  }

  @Override
  protected boolean showTitle() {
    return (!remaining.isZero() && remaining.getSeconds() <= 3) || super.showTitle();
  }
}
