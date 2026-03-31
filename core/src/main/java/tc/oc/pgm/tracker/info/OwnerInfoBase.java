package tc.oc.pgm.tracker.info;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.OwnerInfo;

public abstract class OwnerInfoBase implements OwnerInfo {
  private final @Nullable ParticipantState owner;

  public OwnerInfoBase(@Nullable ParticipantState owner) {
    this.owner = owner;
  }

  @Override
  public @Nullable ParticipantState owner() {
    return owner;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{owner=" + owner() + "}";
  }
}
