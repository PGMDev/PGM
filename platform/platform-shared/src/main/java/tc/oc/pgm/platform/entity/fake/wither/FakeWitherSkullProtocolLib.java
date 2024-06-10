package tc.oc.pgm.platform.entity.fake.wither;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.platform.entity.fake.FakeEntityProtocolLib;
import tc.oc.pgm.util.nms.NMSHacks;

public class FakeWitherSkullProtocolLib extends FakeEntityProtocolLib {

  @Override
  public void spawn(Player viewer, Location location, Vector velocity) {
    NMSHacks.sendSpawnEntityPacket(viewer.getPlayer(), entityId(), location, velocity);
  }
}
