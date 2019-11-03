package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import tc.oc.pgm.PGM;
import tc.oc.pgm.api.match.Match;

public class EntityQuery extends Query implements IEntityTypeQuery {

  private final Entity entity;

  public EntityQuery(@Nullable Event event, Entity entity) {
    super(event);
    this.entity = checkNotNull(entity);
  }

  @Override
  public Class<? extends Entity> getEntityType() {
    return entity.getClass();
  }

  @Override
  public Match getMatch() {
    return PGM.getMatchManager().getMatch(entity.getWorld());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EntityQuery)) return false;
    EntityQuery query = (EntityQuery) o;
    if (!entity.equals(query.entity)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return entity.hashCode();
  }
}
