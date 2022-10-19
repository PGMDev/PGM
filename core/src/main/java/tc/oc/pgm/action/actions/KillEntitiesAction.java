package tc.oc.pgm.action.actions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ItemDespawnEvent;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.query.EntityQuery;

public class KillEntitiesAction extends AbstractAction<Match> {

  private final Filter filter;

  public KillEntitiesAction(Filter filter) {
    super(Match.class);
    this.filter = filter;
  }

  @Override
  public void trigger(Match match) {
    match
        .getWorld()
        .getEntities()
        .forEach(
            (Entity entity) -> {
              if (!(entity instanceof Player)
                  && filter.query(new EntityQuery(null, entity)).isAllowed()) {
                if (entity instanceof Item) {
                  match.callEvent(new ItemDespawnEvent((Item) entity, entity.getLocation()));
                }
                entity.remove();
              }
            });
  }
}
