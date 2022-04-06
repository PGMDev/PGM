package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;

public interface DamageQuery extends MatchQuery {
  ParticipantState getVictim();

  DamageInfo getDamageInfo();
}
