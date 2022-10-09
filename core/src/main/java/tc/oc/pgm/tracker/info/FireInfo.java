package tc.oc.pgm.tracker.info;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

public class FireInfo implements OwnerInfo, CauseInfo, DamageInfo {

  private final @Nullable PhysicalInfo igniter;

  public FireInfo(@Nullable PhysicalInfo igniter) {
    this.igniter = igniter;
  }

  public FireInfo() {
    this(null);
  }

  public @Nullable PhysicalInfo getIgniter() {
    return igniter;
  }

  @Override
  public PhysicalInfo getCause() {
    return getIgniter();
  }

  @Override
  public @Nullable ParticipantState getOwner() {
    return igniter == null ? null : igniter.getOwner();
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{igniter=" + igniter + "}";
  }
}
