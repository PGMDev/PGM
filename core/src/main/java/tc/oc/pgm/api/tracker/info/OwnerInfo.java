package tc.oc.pgm.api.tracker.info;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public interface OwnerInfo extends TrackerInfo {
  @Nullable
  ParticipantState owner();

  @Deprecated
  default @Nullable ParticipantState getOwner() {
    return owner();
  }
}
