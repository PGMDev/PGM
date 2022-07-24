package tc.oc.pgm.filters.matcher.entity;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import tc.oc.pgm.api.filter.query.EntitySpawnQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class SpawnReasonFilter extends TypedFilter.Impl<EntitySpawnQuery> {
  protected final SpawnReason reason;

  public SpawnReasonFilter(SpawnReason reason) {
    this.reason = reason;
  }

  @Override
  public Class<? extends EntitySpawnQuery> queryType() {
    return EntitySpawnQuery.class;
  }

  @Override
  public boolean matches(EntitySpawnQuery query) {
    return reason == query.getSpawnReason();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{reason=" + this.reason + "}";
  }
}
