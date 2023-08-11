package tc.oc.pgm.util.nms.entity.fake;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FakeEntityNoOp implements FakeEntity {

  @Override
  public int entityId() {
    return 0;
  }

  @Override
  public void spawn(Player viewer, Location location, Vector velocity) {}
}
