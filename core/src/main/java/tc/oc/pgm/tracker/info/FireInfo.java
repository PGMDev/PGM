package tc.oc.pgm.tracker.info;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

public record FireInfo(@Nullable PhysicalInfo igniter) implements OwnerInfo, CauseInfo, DamageInfo {

  public FireInfo() {
    this(null);
  }

  @Deprecated
  public @Nullable PhysicalInfo getIgniter() {
    return igniter();
  }

  @Override
  public @Nullable PhysicalInfo cause() {
    return igniter();
  }

  @Override
  public @Nullable ParticipantState owner() {
    return igniter == null ? null : igniter.owner();
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return owner();
  }

  @Override
  public @NonNull String toString() {
    return getClass().getSimpleName() + "{igniter=" + igniter + "}";
  }
}
