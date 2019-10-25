package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import tc.oc.pgm.match.ParticipantState;

public interface DamageInfo extends TrackerInfo {
  @Nullable
  ParticipantState getAttacker();
}
