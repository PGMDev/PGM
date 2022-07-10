package tc.oc.pgm.action.actions;

import org.bukkit.entity.Entity;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.query.EntityQuery;

public class KillEntitiesAction extends AbstractAction<Match> {

  Filter filter;

  public KillEntitiesAction(Filter filter) {
    super(Match.class);
    this.filter = filter;
  }

  @Override
  public void trigger(Match m) {
    m.getWorld()
        .getEntities()
        .forEach(
            (Entity entity) -> {
              if (filter.query(new EntityQuery(null, entity)).isAllowed()) {
                entity.remove();
              }
            });
  }
}
