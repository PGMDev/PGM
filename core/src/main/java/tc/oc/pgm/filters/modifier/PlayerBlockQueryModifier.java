package tc.oc.pgm.filters.modifier;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PlayerQuery;

public class PlayerBlockQueryModifier extends QueryModifier<PlayerQuery, PlayerQuery> {

  public PlayerBlockQueryModifier(Filter filter) {
    super(filter, PlayerQuery.class);
  }

  @Nullable
  @Override
  protected PlayerQuery transformQuery(PlayerQuery query) {
    // Deny when no player can be found, eg: they disconnected
    if (query.getPlayer() == null) return null;

    return query.getPlayer();
  }

  @Override
  public Class<? extends PlayerQuery> queryType() {
    return PlayerQuery.class;
  }
}
