package tc.oc.pgm.variables.types;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.timelimit.TimeLimit;
import tc.oc.pgm.timelimit.TimeLimitMatchModule;

public class TimeLimitVariable extends AbstractVariable<Match>
    implements StateHolder<TimeLimitVariable.State> {

  public static final TimeLimitVariable INSTANCE = new TimeLimitVariable();

  public TimeLimitVariable() {
    super(Match.class);
  }

  @Override
  public void load(Match match) {
    match
        .getFeatureContext()
        .registerState(
            this,
            new State(
                match.moduleRequire(TimeLimitMatchModule.class),
                new TimeLimit(
                    null, Duration.of(0, ChronoUnit.SECONDS), null, null, null, null, true)));
  }

  @Override
  protected double getValueImpl(Match obj) {
    Duration remaining = obj.state(this).tlmm.getFinalRemaining();
    return remaining == null ? -1 : remaining.getSeconds();
  }

  @Override
  protected void setValueImpl(Match obj, double value) {
    var state = obj.state(this);
    TimeLimit existingTimeLimit = state.tlmm.getTimeLimit();
    if (value < 0) {
      if (existingTimeLimit != null) {
        state.oldTimeLimit = existingTimeLimit;
      }

      state.tlmm.cancel();
      return;
    }

    TimeLimit newTimeLimit = new TimeLimit(
        existingTimeLimit != null ? existingTimeLimit : state.oldTimeLimit,
        Duration.of((long) value, ChronoUnit.SECONDS));
    state.tlmm.setTimeLimit(newTimeLimit);
    state.tlmm.start();
  }

  public static class State {
    private final TimeLimitMatchModule tlmm;
    private TimeLimit oldTimeLimit;

    public State(TimeLimitMatchModule tlmm, TimeLimit oldTimeLimit) {
      this.tlmm = tlmm;
      this.oldTimeLimit = oldTimeLimit;
    }
  }
}
