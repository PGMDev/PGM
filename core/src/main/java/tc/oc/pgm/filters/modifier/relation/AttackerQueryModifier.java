package tc.oc.pgm.filters.modifier.relation;

import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class AttackerQueryModifier extends QueryModifier<DamageQuery> {

  public AttackerQueryModifier(Filter child) {
    super(child);
  }

  @Nullable
  @Override
  protected Query modifyQuery(DamageQuery query) {
    ParticipantState attacker = query.getDamageInfo().getAttacker();
    if (attacker == null) {
      // It's not at all clear what is the best thing to do in this case,
      // but this seems to make sense. No player is available to query,
      // so pass through a non-player query.
      return new MatchQuery(query.getEvent(), query.getMatch());
    } else {
      return new PlayerStateQuery(query.getEvent(), attacker);
    }
  }

  @Override
  public Class<? extends DamageQuery> getQueryType() {
    return DamageQuery.class;
  }
}
