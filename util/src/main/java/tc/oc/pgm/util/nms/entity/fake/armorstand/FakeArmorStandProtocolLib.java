package tc.oc.pgm.util.nms.entity.fake.armorstand;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.nms.entity.fake.FakeEntityProtocolLib;

public class FakeArmorStandProtocolLib extends FakeEntityProtocolLib {

  ItemStack head;

  public FakeArmorStandProtocolLib(ItemStack head) {
    this.head = head;
  }

  @Override
  public void spawn(Player viewer, Location location, Vector velocity) {
    NMSHacks.spawnFakeArmorStand(viewer.getPlayer(), entityId(), location, velocity);
    wear(viewer, 4, head);
  }
}
