package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.filters.query.IDamageQuery;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class VictimFilter extends TypedFilter<IDamageQuery> {

  private final Filter child;

  public VictimFilter(Filter child) {
    this.child = checkNotNull(child);
  }

  @Override
  public Class<? extends IDamageQuery> getQueryType() {
    return IDamageQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IDamageQuery query) {
    return child.query(new PlayerStateQuery(query.getEvent(), query.getVictim()));
  }
}
