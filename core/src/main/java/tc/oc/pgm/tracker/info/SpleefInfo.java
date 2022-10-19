package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;

public class SpleefInfo implements DamageInfo, CauseInfo {

  private final DamageInfo breaker;
  private final Tick time;

  public SpleefInfo(DamageInfo breaker, Tick time) {
    this.breaker = assertNotNull(breaker);
    this.time = assertNotNull(time);
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

  public Tick getTime() {
    return time;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{time=" + getTime() + " breaker=" + getBreaker() + "}";
  }
}
