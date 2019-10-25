package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IDamageQuery;
import tc.oc.pgm.match.PlayerRelation;

public class RelationFilter extends TypedFilter<IDamageQuery> {

  private final PlayerRelation relation;

  public RelationFilter(PlayerRelation relation) {
    this.relation = relation;
  }

  @Override
  public Class<? extends IDamageQuery> getQueryType() {
    return IDamageQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IDamageQuery query) {
    return QueryResponse.fromBoolean(
        relation.are(query.getVictim(), query.getDamageInfo().getAttacker()));
  }
}
