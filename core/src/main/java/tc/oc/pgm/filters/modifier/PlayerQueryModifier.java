package tc.oc.pgm.filters.modifier;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PlayerQuery;

public class PlayerQueryModifier extends QueryModifier<PlayerQuery, PlayerQuery> {

  public PlayerQueryModifier(Filter filter) {
    super(filter, PlayerQuery.class);
  }

  @Nullable
  @Override
  protected PlayerQuery transformQuery(PlayerQuery query) {
    // Intentionally drop all query information (eg:what block was affected) and keep just the
    // player.
    return query.getPlayer();
  }

  @Override
  public Class<? extends PlayerQuery> queryType() {
    return PlayerQuery.class;
  }
}
