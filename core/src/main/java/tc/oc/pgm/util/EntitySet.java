package tc.oc.pgm.util;

import org.bukkit.entity.Entity;
import tc.oc.pgm.api.match.Match;

/** Match-scoped set of entities, see {@link EntityMap}. */
public class EntitySet {

  private final EntityMap<Boolean> entities;

  public EntitySet(Match match) {
    this.entities = new EntityMap<>(match);
  }

  public boolean contains(Entity entity) {
    return entities.contains(entity);
  }

  public void add(Entity entity) {
    entities.put(entity, Boolean.TRUE);
  }
}
