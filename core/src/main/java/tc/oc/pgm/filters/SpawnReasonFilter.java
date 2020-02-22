package tc.oc.pgm.filters;

import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import tc.oc.pgm.filters.query.IEntitySpawnQuery;

public class SpawnReasonFilter extends TypedFilter<IEntitySpawnQuery> {
  protected final SpawnReason reason;

  public SpawnReasonFilter(SpawnReason reason) {
    this.reason = reason;
  }

  @Override
  public Class<? extends IEntitySpawnQuery> getQueryType() {
    return IEntitySpawnQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IEntitySpawnQuery query) {
    return QueryResponse.fromBoolean(reason == query.getSpawnReason());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{reason=" + this.reason + "}";
  }
}
