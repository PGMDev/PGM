package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import tc.oc.pgm.match.ParticipantState;
import tc.oc.pgm.time.TickTime;

public class SpleefInfo implements DamageInfo, CauseInfo {

  private final DamageInfo breaker;
  private final TickTime time;

  public SpleefInfo(DamageInfo breaker, TickTime time) {
    this.breaker = checkNotNull(breaker);
    this.time = checkNotNull(time);
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getBreaker().getAttacker();
  }

  @Override
  public DamageInfo getCause() {
    return breaker;
  }

  public DamageInfo getBreaker() {
    return breaker;
  }

  public TickTime getTime() {
    return time;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{time=" + getTime() + " breaker=" + getBreaker() + "}";
  }
}
