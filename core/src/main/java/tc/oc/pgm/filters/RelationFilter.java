package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.player.PlayerRelation;

public class RelationFilter extends TypedFilter<DamageQuery> {

  private final PlayerRelation relation;

  public RelationFilter(PlayerRelation relation) {
    this.relation = relation;
  }

  @Override
  public Class<? extends DamageQuery> getQueryType() {
    return DamageQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(DamageQuery query) {
    return QueryResponse.fromBoolean(
        relation.are(query.getVictim(), query.getDamageInfo().getAttacker()));
  }
}
