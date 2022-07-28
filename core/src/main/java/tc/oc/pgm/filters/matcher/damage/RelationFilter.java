package tc.oc.pgm.filters.matcher.damage;

import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class RelationFilter extends TypedFilter.Impl<DamageQuery> {

  private final PlayerRelation relation;

  public RelationFilter(PlayerRelation relation) {
    this.relation = relation;
  }

  @Override
  public Class<? extends DamageQuery> queryType() {
    return DamageQuery.class;
  }

  @Override
  public boolean matches(DamageQuery query) {
    return relation.are(query.getVictim(), query.getDamageInfo().getAttacker());
  }
}
