package tc.oc.pgm.timeadjust;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public class TimeAdjust {

  private Duration time;
  private boolean broadcast;

  public TimeAdjust(Duration time, boolean broadcast) {
    this.time = checkNotNull(time);
    this.broadcast = broadcast;
  }

  public Duration getTime() {
    return time;
  }

  public boolean isBroadcast() {
    return broadcast;
  }

  private boolean isTimeSafe(Duration current) {
    return !current.plus(time).isNegative();
  }

  public boolean adjustTime(Match match) {
    TimeLimitMatchModule timeLimit = match.needModule(TimeLimitMatchModule.class);
    if (timeLimit.getFinalRemaining() != null) {
      // If we are able to add/subtract time to the match, do so.
      // In the case of the time being subtracted to a negative, trigger match end.
      if (isTimeSafe(timeLimit.getFinalRemaining())) {
        TimeLimit adjusted = getNewTime(timeLimit.getFinalRemaining(), timeLimit.getTimeLimit());
        timeLimit.cancel();
        timeLimit.setTimeLimit(adjusted);
        timeLimit.start();
        return isBroadcast();
      } else {
        timeLimit.setFinished(true);
        match.calculateVictory();
      }
    }
    return false;
  }

  private TimeLimit getNewTime(Duration finalTime, TimeLimit oldLimit) {
    Duration adjustedTime = finalTime.plus(time);
    return new TimeLimit(
        oldLimit.getId(),
        adjustedTime,
        oldLimit.getOvertime(),
        oldLimit.getMaxOvertime(),
        oldLimit.getEndOvertime(),
        oldLimit.getResult(),
        oldLimit.getShow());
  }

  @Override
  public String toString() {
    return String.format("{TimeAdjust: time=%s broadcast=%s}", time.toString(), broadcast);
  }
}
