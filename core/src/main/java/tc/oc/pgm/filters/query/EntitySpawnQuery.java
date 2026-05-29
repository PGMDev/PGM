package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jspecify.annotations.Nullable;

public class EntitySpawnQuery extends EntityQuery
    implements tc.oc.pgm.api.filter.query.EntitySpawnQuery {

  private final CreatureSpawnEvent.SpawnReason spawnReason;

  public EntitySpawnQuery(
      @Nullable Event event, Entity entity, CreatureSpawnEvent.SpawnReason spawnReason) {
    super(event, entity);
    this.spawnReason = assertNotNull(spawnReason);
  }

  @Override
  public CreatureSpawnEvent.SpawnReason getSpawnReason() {
    return spawnReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EntitySpawnQuery query)) return false;
    if (!super.equals(o)) return false;
    return spawnReason == query.spawnReason;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + spawnReason.hashCode();
    return result;
  }
}
