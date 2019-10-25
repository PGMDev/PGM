package tc.oc.pgm.timelimit;

import javax.annotation.Nullable;
import org.joda.time.Duration;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;

public class TimeLimitMatchModule extends MatchModule {
  private final TimeLimit defaultTimeLimit;
  private @Nullable TimeLimit timeLimit;
  private @Nullable TimeLimitCountdown countdown;

  public TimeLimitMatchModule(Match match, @Nullable TimeLimit timeLimit) {
    super(match);
    this.defaultTimeLimit = timeLimit;
  }

  @Override
  public void load() {
    super.load();
    setTimeLimit(defaultTimeLimit);
  }

  @Override
  public void enable() {
    this.start();
  }

  public @Nullable TimeLimit getTimeLimit() {
    return this.timeLimit;
  }

  public void setTimeLimit(@Nullable TimeLimit timeLimit) {
    if (timeLimit != this.timeLimit) {
      logger.fine("Changing time limit to " + timeLimit);

      this.timeLimit = timeLimit;
      getMatch().removeVictoryConditions(TimeLimit.class);
      if (this.timeLimit != null) {
        getMatch().addVictoryCondition(this.timeLimit);
      }
    }
  }

  public @Nullable TimeLimitCountdown getCountdown() {
    return countdown;
  }

  public @Nullable Duration getFinalRemaining() {
    return this.countdown == null ? null : this.countdown.getRemaining();
  }

  public void start() {
    // Match.end() will cancel this, so we don't have to
    if (this.timeLimit != null && this.getMatch().isRunning()) {
      this.countdown = new TimeLimitCountdown(this.getMatch(), this.timeLimit);
      this.countdown.start();
    }
  }

  public void cancel() {
    if (this.countdown != null) {
      this.countdown.cancel();
      this.countdown = null;
    }
  }
}
