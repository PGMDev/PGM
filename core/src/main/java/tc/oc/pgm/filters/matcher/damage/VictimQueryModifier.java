package tc.oc.pgm.filters.matcher.damage;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.DamageQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.filters.query.PlayerStateQuery;

public class VictimQueryModifier extends QueryModifier<DamageQuery, PlayerQuery> {

  public VictimQueryModifier(Filter child) {
    super(child, PlayerQuery.class);
  }

  @Nullable
  @Override
  protected PlayerQuery transformQuery(DamageQuery query) {
    return new PlayerStateQuery(query.getEvent(), query.getVictim());
  }

  @Override
  public Class<? extends DamageQuery> queryType() {
    return DamageQuery.class;
  }
}
