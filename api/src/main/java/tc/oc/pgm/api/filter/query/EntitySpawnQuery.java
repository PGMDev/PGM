package tc.oc.pgm.api.filter.query;

import org.bukkit.event.entity.CreatureSpawnEvent;

public interface EntitySpawnQuery extends EntityTypeQuery {
  CreatureSpawnEvent.SpawnReason getSpawnReason();
}
