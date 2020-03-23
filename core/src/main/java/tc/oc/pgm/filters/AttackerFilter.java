package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.filters.query.MatchQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class AttackerFilter extends TypedFilter<DamageQuery> {

  private final Filter child;

  public AttackerFilter(Filter child) {
    this.child = child;
  }

  @Override
  public Class<? extends DamageQuery> getQueryType() {
    return DamageQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(DamageQuery query) {
    ParticipantState attacker = query.getDamageInfo().getAttacker();
    if (attacker == null) {
      // It's not at all clear what is the best thing to do in this case,
      // but this seems to make sense. No player is available to query,
      // so pass through a non-player query.
      return child.query(new MatchQuery(query.getEvent(), query.getMatch()));
    } else {
      return child.query(new PlayerStateQuery(query.getEvent(), attacker));
    }
  }
}
