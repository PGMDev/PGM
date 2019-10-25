package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import tc.oc.pgm.match.ParticipantState;

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
