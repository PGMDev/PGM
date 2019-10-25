package tc.oc.pgm.start;

import org.joda.time.Duration;
import tc.oc.chat.Sound;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchState;

/** Common base for countdowns leading up to match start */
public abstract class PreMatchCountdown extends MatchCountdown {

  protected static final Sound COUNT_SOUND = new Sound("note.pling", 1f, 1.19f);

  public PreMatchCountdown(Match match) {
    super(match);
  }

  @Override
  public void onStart(Duration remaining, Duration total) {
    super.onStart(remaining, total);
    getMatch().setState(MatchState.Starting);
  }

  @Override
  public void onTick(Duration remaining, Duration total) {
    super.onTick(remaining, total);
    if (remaining.getStandardSeconds() >= 1 && remaining.getStandardSeconds() <= 3) {
      getMatch().playSound(COUNT_SOUND);
    }
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    getMatch().setState(MatchState.Idle);
    getMatch().sendWarning(new PersonalizedTranslatable("broadcast.startCancelled"), false);
  }

  @Override
  protected boolean showTitle() {
    return (remaining.isLongerThan(Duration.ZERO)
            && !remaining.isLongerThan(Duration.standardSeconds(3)))
        || super.showTitle();
  }
}
