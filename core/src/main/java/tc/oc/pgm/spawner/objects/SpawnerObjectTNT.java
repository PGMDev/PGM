package tc.oc.pgm.spawner.objects;

import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;
import tc.oc.pgm.spawner.SpawnerObject;

public class SpawnerObjectTNT implements SpawnerObject {

  private final float power;
  private final int fuse;
  private final int count;

  public SpawnerObjectTNT(float power, int fuse, int count) {
    this.power = power;
    this.fuse = fuse;
    this.count = count;
  }

  @Override
  public void spawn(Location location) {
    for (int i = 0; i < count; i++) {
      TNTPrimed primed = location.getWorld().spawn(location, TNTPrimed.class);
      primed.setFuseTicks(fuse);
      primed.setYield(power);
    }
  }

  @Override
  public boolean isTracked() {
    return false;
  }

  @Override
  public int spawnCount() {
    return count;
  }
}
