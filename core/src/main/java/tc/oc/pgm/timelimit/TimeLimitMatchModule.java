package tc.oc.pgm.timelimit;

import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.party.VictoryCondition;

public class TimeLimitMatchModule implements MatchModule {
  private final Match match;
  private final TimeLimit defaultTimeLimit;
  private @Nullable TimeLimit timeLimit;
  private @Nullable TimeLimitCountdown countdown;
  private @Nullable OvertimeCountdown overtime;
  private boolean finished; // If Time limit ended this match

  public TimeLimitMatchModule(Match match, @Nullable TimeLimit timeLimit) {
    this.match = match;
    this.defaultTimeLimit = timeLimit;
  }

  @Override
  public void load() {
    setTimeLimit(defaultTimeLimit);
  }

  @Override
  public void enable() {
    this.start();
  }

  public boolean isFinished() {
    return finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public @Nullable TimeLimit getTimeLimit() {
    return this.timeLimit;
  }

  public void setTimeLimit(@Nullable TimeLimit timeLimit) {
    if (timeLimit != this.timeLimit) {
      match.getLogger().fine("Changing time limit to " + timeLimit);

      this.timeLimit = timeLimit;
      for (VictoryCondition condition : match.getVictoryConditions()) {
        if (condition instanceof TimeLimit) {
          match.removeVictoryCondition(condition);
        }
      }
      if (this.timeLimit != null) {
        match.addVictoryCondition(this.timeLimit);
      }
    }
  }

  public @Nullable TimeLimitCountdown getCountdown() {
    return countdown != null ? countdown : overtime;
  }

  public @Nullable Duration getFinalRemaining() {
    return this.countdown == null ? null : this.countdown.getRemaining();
  }

  public void start() {
    cancel();

    // Match.finish() will cancel this, so we don't have to
    if (this.timeLimit != null && match.isRunning()) {
      this.countdown = new TimeLimitCountdown(match, this.timeLimit);
      this.countdown.start();
    }
  }

  public void startOvertime() {
    cancel();
    if (this.timeLimit != null && this.timeLimit.getOvertime() != null && match.isRunning()) {
      this.overtime = new OvertimeCountdown(match, this.timeLimit);
      this.overtime.start();
    }
  }

  public void cancel() {
    if (this.countdown != null) {
      this.countdown.cancel();
      this.countdown = null;
    }
    if (this.overtime != null) {
      this.overtime.cancel();
      this.overtime = null;
    }
  }
}
