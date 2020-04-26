package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import tc.oc.pgm.spawner.SpawnerObject;

public class SpawnerObjectTNT implements SpawnerObject {

    private final float power;
    private final int fuse;

    public SpawnerObjectTNT(float power, int fuse) {
        this.power = power;
        this.fuse = fuse;
    }

    @Override
    public void spawn(Location location) {
        TNTPrimed primed = (TNTPrimed) location.getWorld().spawnEntity(location, EntityType.PRIMED_TNT);
        primed.setFuseTicks(fuse);
        primed.setYield(power);
    }

    @Override
    public boolean isTracked() {
        return false;
    }
}
