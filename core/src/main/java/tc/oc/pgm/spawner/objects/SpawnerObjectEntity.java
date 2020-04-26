package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.spawner.SpawnerModule;
import tc.oc.pgm.spawner.SpawnerObject;

public class SpawnerObjectEntity implements SpawnerObject {

  private Class<? extends Entity> entity;
  private int spawnCount;

  public SpawnerObjectEntity(Class<? extends Entity> entity, int spawnCount) {
    this.entity = entity;
    this.spawnCount = spawnCount;
  }

  @Override
  public void spawn(Location location) {
    for (int i = 0; i < spawnCount; i++) {
      Entity spawned = location.getWorld().spawn(location, entity);
      spawned.setMetadata(
          SpawnerModule.METADATA_KEY, new FixedMetadataValue(PGM.get(), "Spawner Entity"));
    }
  }

  @Override
  public boolean isTracked() {
    return true;
  }

  @Override
  public int spawnCount() {
    return spawnCount;
  }
}
