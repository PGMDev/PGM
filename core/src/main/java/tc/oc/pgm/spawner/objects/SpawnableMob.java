package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;

public class SpawnableMob implements Spawnable {

  private final SpawnableEntity entity;
  private final String spawnerId;

  public SpawnableMob(SpawnableEntity entity, String spawnerId) {
    this.entity = entity;
    this.spawnerId = spawnerId;
  }

  @Override
  public void spawn(Location location, Match match) {
    Entity spawned = entity.spawn(location);
    spawned.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), spawnerId));
  }

  @Override
  public int getSpawnCount() {
    return 1;
  }
}
