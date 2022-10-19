package tc.oc.pgm.filters.matcher.player;

import static tc.oc.pgm.util.Assert.assertNotNull;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class PlayerClassFilter extends TypedFilter.Impl<PlayerQuery> {
  protected final PlayerClass playerClass;

  public PlayerClassFilter(PlayerClass playerClass) {
    this.playerClass = assertNotNull(playerClass, "player class");
  }

  @Override
  public Class<? extends PlayerQuery> queryType() {
    return PlayerQuery.class;
  }

  @Override
  public boolean matches(PlayerQuery query) {
    return query
        .moduleOptional(ClassMatchModule.class)
        .map(cmm -> this.playerClass.equals(cmm.getPlayingClass(query.getId())))
        .orElse(false);
  }
}
