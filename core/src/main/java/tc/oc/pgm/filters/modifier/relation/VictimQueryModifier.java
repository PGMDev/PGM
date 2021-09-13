package tc.oc.pgm.filters.modifier.relation;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class VictimQueryModifier extends QueryModifier<DamageQuery> {

  public VictimQueryModifier(Filter child) {
    super(child);
  }

  @Nullable
  @Override
  protected Query modifyQuery(DamageQuery query) {
    return new PlayerStateQuery(query.getEvent(), query.getVictim());
  }

  @Override
  public Class<? extends DamageQuery> getQueryType() {
    return DamageQuery.class;
  }
}
