package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class VictimFilter extends TypedFilter<DamageQuery> {

  private final Filter child;

  public VictimFilter(Filter child) {
    this.child = checkNotNull(child);
  }

  @Override
  public Class<? extends DamageQuery> getQueryType() {
    return DamageQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(DamageQuery query) {
    return child.query(new PlayerStateQuery(query.getEvent(), query.getVictim()));
  }
}
