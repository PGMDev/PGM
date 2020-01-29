package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.filters.query.IPlayerQuery;

public class PlayerClassFilter extends TypedFilter<IPlayerQuery> {
  protected final PlayerClass playerClass;

  public PlayerClassFilter(PlayerClass playerClass) {
    this.playerClass = Preconditions.checkNotNull(playerClass, "player class");
  }

  @Override
  public Class<? extends IPlayerQuery> getQueryType() {
    return IPlayerQuery.class;
  }

  @Override
  public QueryResponse queryTyped(IPlayerQuery query) {
    ClassMatchModule classes = query.getMatch().getModule(ClassMatchModule.class);
    return QueryResponse.fromBoolean(
        classes != null && this.playerClass.equals(classes.getPlayingClass(query.getPlayerId())));
  }
}
