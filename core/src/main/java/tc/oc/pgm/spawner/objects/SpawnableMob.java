package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.jdom2.Element;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.entity.SpawnableEntity;
import tc.oc.pgm.spawner.Spawnable;
import tc.oc.pgm.spawner.Spawner;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class SpawnableMob implements Spawnable {

  private final SpawnableEntity entity;
  private final String spawnerId;
  private final Element source;

  public SpawnableMob(SpawnableEntity entity, String spawnerId, Element source) {
    this.entity = entity;
    this.spawnerId = spawnerId;
    this.source = source;
  }

  @Override
  public void spawn(Location location, Match match) {
    Entity spawned = entity.spawn(location);
    spawned.setMetadata(Spawner.METADATA_KEY, new FixedMetadataValue(PGM.get(), spawnerId));
  }

  public void validate() throws InvalidXMLException {
    entity.validate(source);
  }

  @Override
  public int getSpawnCount() {
    return 1;
  }
}
