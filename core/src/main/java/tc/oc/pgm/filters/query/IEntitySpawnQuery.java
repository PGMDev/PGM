package tc.oc.pgm.filters.query;

import org.bukkit.event.entity.CreatureSpawnEvent;

public interface IEntitySpawnQuery extends IEntityTypeQuery {
  CreatureSpawnEvent.SpawnReason getSpawnReason();
}
