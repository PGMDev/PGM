package tc.oc.pgm.api.tracker.info;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public interface DamageInfo extends TrackerInfo {
  @Nullable
  ParticipantState getAttacker();
}
