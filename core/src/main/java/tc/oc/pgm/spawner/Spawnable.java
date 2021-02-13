package tc.oc.pgm.spawner;

import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;

public interface Spawnable {

  void spawn(Location location, Match match);

  int getSpawnCount();
}
