package tc.oc.pgm.filters.modifier;

import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.filter.query.Query;

public class PlayerBlockQueryModifier extends QueryModifier<PlayerQuery> {

  public PlayerBlockQueryModifier(Filter filter) {
    super(filter);
  }

  @Nullable
  @Override
  protected Query modifyQuery(PlayerQuery query) {
    // Abstain when no player can be found, eg: they disconnected
    if (query.getPlayer() == null) return null;

    return query.getPlayer().getQuery();
  }

  @Override
  public Class<? extends PlayerQuery> getQueryType() {
    return PlayerQuery.class;
  }
}
