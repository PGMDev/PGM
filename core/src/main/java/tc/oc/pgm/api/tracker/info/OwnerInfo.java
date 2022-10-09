package tc.oc.pgm.api.tracker.info;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public interface OwnerInfo extends TrackerInfo {
  @Nullable
  ParticipantState getOwner();
}
