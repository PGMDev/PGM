package tc.oc.pgm.start;

import org.joda.time.Duration;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.countdowns.MatchCountdown;

/** Common base for countdowns leading up to match start */
public abstract class PreMatchCountdown extends MatchCountdown {

  protected static final Sound COUNT_SOUND = new Sound("note.pling", 1f, 1.19f);

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
    if (remaining.getStandardSeconds() >= 1 && remaining.getStandardSeconds() <= 3) {
      getMatch().playSound(COUNT_SOUND);
    }
  }

  @Override
  public void onCancel(Duration remaining, Duration total) {
    super.onCancel(remaining, total);
    getMatch().setPhase(MatchPhase.IDLE);
    getMatch().sendWarning(new PersonalizedTranslatable("broadcast.startCancelled"), false);
  }

  @Override
  protected boolean showTitle() {
    return (remaining.isLongerThan(Duration.ZERO)
            && !remaining.isLongerThan(Duration.standardSeconds(3)))
        || super.showTitle();
  }
}
