package tc.oc.pgm.api.tracker.info;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public interface DamageInfo extends TrackerInfo {

  @Nullable
  default PhysicalInfo damager() {
    return null;
  }

  @Deprecated
  default @Nullable PhysicalInfo getDamager() {
    return damager();
  }

  @Nullable
  ParticipantState attacker();

  @Deprecated
  default @Nullable ParticipantState getAttacker() {
    return attacker();
  }
}
