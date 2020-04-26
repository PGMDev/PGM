package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.spawner.SpawnerObject;

public class SpawnerObjectEntity implements SpawnerObject {

    Class<? extends Entity> entity;

    public SpawnerObjectEntity(Class<? extends Entity> entity) {
        this.entity = entity;
    }

    @Override
    public void spawn(Location location) {
        Entity spawned = location.getWorld().spawn(location, entity);
    }
}
