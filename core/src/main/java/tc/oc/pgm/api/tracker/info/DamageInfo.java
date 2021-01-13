package tc.oc.pgm.api.tracker.info;

import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public interface DamageInfo extends TrackerInfo {
  @Nullable
  ParticipantState getAttacker();
}
