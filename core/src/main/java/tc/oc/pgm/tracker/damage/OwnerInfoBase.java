package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public abstract class OwnerInfoBase implements OwnerInfo {
  private final @Nullable ParticipantState owner;

  public OwnerInfoBase(@Nullable ParticipantState owner) {
    this.owner = owner;
  }

  @Override
  public @Nullable ParticipantState getOwner() {
    return owner;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{owner=" + getOwner() + "}";
  }
}
