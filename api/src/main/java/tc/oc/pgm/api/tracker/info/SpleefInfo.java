package tc.oc.pgm.api.tracker.info;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;

public interface SpleefInfo extends DamageInfo, CauseInfo {
  @Override
  @Nullable
  ParticipantState getAttacker();

  @Override
  DamageInfo getCause();

  DamageInfo getBreaker();

  Tick getTime();

  @Override
  String toString();
}
