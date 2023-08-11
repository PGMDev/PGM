package tc.oc.pgm.util.nms.entity.fake;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.NMSHacks;

public interface FakeEntity {
  int entityId();

  void spawn(Player viewer, Location location, org.bukkit.util.Vector velocity);

  default void spawn(Player viewer, Location location) {
    spawn(viewer, location, new org.bukkit.util.Vector(0, 0, 0));
  }

  default void destroy(Player viewer) {
    int[] entityIds = new int[] {entityId()};
    NMSHacks.sendPacket(viewer, NMSHacks.destroyEntitiesPacket(entityIds));
  }

  default void teleport(Player viewer, Location location) {
    NMSHacks.sendPacket(viewer, NMSHacks.teleportEntityPacket(entityId(), location));
  }

  default void ride(Player viewer, int riderID) {
    int vehicleID = entityId();
    NMSHacks.entityAttach(viewer, riderID, vehicleID, false);
  }

  default void mount(Player viewer, Entity vehicle) {
    int entityID = entityId();
    int vehicleID = vehicle.getEntityId();
    NMSHacks.entityAttach(viewer, entityID, vehicleID, false);
  }

  default void wear(Player viewer, int slot, ItemStack item) {
    NMSHacks.sendPacket(viewer, NMSHacks.entityEquipmentPacket(entityId(), slot, item));
  }
}
