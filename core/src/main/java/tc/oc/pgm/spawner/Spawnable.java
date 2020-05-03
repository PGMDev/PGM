package tc.oc.pgm.spawner;

import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;

public interface Spawnable {

  void spawn(Location location, Match match);

  /**
   * Some objects are tracked and added to the max entities count (Mobs, items) while others
   * (ThrownPotions, TNT) are not.
   *
   * @return whether the object affects the entity count.
   */
  boolean isTracked();

  int getSpawnCount();
}
