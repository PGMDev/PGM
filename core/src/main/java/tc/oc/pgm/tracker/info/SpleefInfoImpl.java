package tc.oc.pgm.tracker.info;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.SpleefInfo;

public class SpleefInfoImpl implements SpleefInfo {

  private final DamageInfo breaker;
  private final Tick time;

  public SpleefInfoImpl(DamageInfo breaker, Tick time) {
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

  @Override
  public DamageInfo getBreaker() {
    return breaker;
  }

  @Override
  public Tick getTime() {
    return time;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{time=" + getTime() + " breaker=" + getBreaker() + "}";
  }
}
