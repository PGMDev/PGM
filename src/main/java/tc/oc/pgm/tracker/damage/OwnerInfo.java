package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import tc.oc.pgm.match.ParticipantState;

public interface OwnerInfo extends TrackerInfo {
  @Nullable
  ParticipantState getOwner();
}
