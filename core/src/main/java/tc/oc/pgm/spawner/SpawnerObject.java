package tc.oc.pgm.spawner;

import org.bukkit.Location;

public interface SpawnerObject {

  void spawn(Location location);

  /**
   * Some objects are tracked and added to the max entities count (Mobs, items) while others
   * (ThrownPotions, TNT) are not.
   *
   * @return whether the object affects the entity count.
   */
  boolean isTracked();

  int spawnCount();
}
