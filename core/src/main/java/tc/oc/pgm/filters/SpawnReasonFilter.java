package tc.oc.pgm.filters;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import tc.oc.pgm.api.filter.query.EntitySpawnQuery;

public class SpawnReasonFilter extends TypedFilter<EntitySpawnQuery> {
  protected final SpawnReason reason;

  public SpawnReasonFilter(SpawnReason reason) {
    this.reason = reason;
  }

  @Override
  public Class<? extends EntitySpawnQuery> getQueryType() {
    return EntitySpawnQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(EntitySpawnQuery query) {
    return QueryResponse.fromBoolean(reason == query.getSpawnReason());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{reason=" + this.reason + "}";
  }
}
