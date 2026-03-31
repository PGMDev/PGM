package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;

public record SpleefInfo(DamageInfo breaker, Tick time) implements DamageInfo, CauseInfo {

  public SpleefInfo {
    assertNotNull(breaker);
    assertNotNull(time);
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return breaker().attacker();
  }

  @Override
  public DamageInfo cause() {
    return breaker();
  }

  @Deprecated
  public DamageInfo getBreaker() {
    return breaker();
  }

  @Deprecated
  public Tick getTime() {
    return time();
  }

  @Override
  public @NonNull String toString() {
    return getClass().getSimpleName() + "{time=" + time() + " breaker=" + breaker() + "}";
  }
}
