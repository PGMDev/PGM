package tc.oc.pgm.filters.query;

import tc.oc.pgm.match.ParticipantState;
import tc.oc.pgm.tracker.damage.DamageInfo;

public interface IDamageQuery extends IMatchQuery {
  ParticipantState getVictim();

  DamageInfo getDamageInfo();
}
