package tc.oc.pgm.spawner;

import org.bukkit.Location;

public interface SpawnerObject {

    void spawn(Location location);

    boolean isTracked();

    int spawnCount();
}
