package tc.oc.pgm.variables.types;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;
import tc.oc.pgm.variables.VariableDefinition;

public class TimeLimitVariable extends AbstractVariable<Match> {

  private TimeLimit oldTimeLimit;
  private TimeLimitMatchModule tlmm;

  public TimeLimitVariable(VariableDefinition<Match> definition) {
    super(definition);
    oldTimeLimit =
        new TimeLimit(null, Duration.of(0, ChronoUnit.SECONDS), null, null, null, null, true);
  }

  @Override
  public void postLoad(Match match) {
    tlmm = match.moduleRequire(TimeLimitMatchModule.class);
  }

  @Override
  protected double getValueImpl(Match obj) {
    Duration remaining = tlmm.getFinalRemaining();
    return remaining == null ? -1 : remaining.getSeconds();
  }

  @Override
  protected void setValueImpl(Match obj, double value) {
    TimeLimit existingTimeLimit = tlmm.getTimeLimit();
    if (value < 0) {
      if (existingTimeLimit != null) {
        oldTimeLimit = existingTimeLimit;
      }

      tlmm.cancel();
      return;
    }

    TimeLimit newTimeLimit =
        new TimeLimit(
            existingTimeLimit != null ? existingTimeLimit : oldTimeLimit,
            Duration.of((long) value, ChronoUnit.SECONDS));
    tlmm.setTimeLimit(newTimeLimit);
    tlmm.start();
  }
}
