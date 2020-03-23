package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;

public class PlayerClassFilter extends TypedFilter<PlayerQuery> {
  protected final PlayerClass playerClass;

  public PlayerClassFilter(PlayerClass playerClass) {
    this.playerClass = Preconditions.checkNotNull(playerClass, "player class");
  }

  @Override
  public Class<? extends PlayerQuery> getQueryType() {
    return PlayerQuery.class;
  }

  @Override
  public QueryResponse queryTyped(PlayerQuery query) {
    ClassMatchModule classes = query.getMatch().getModule(ClassMatchModule.class);
    return QueryResponse.fromBoolean(
        classes != null && this.playerClass.equals(classes.getPlayingClass(query.getPlayerId())));
  }
}
