package tc.oc.pgm.util.nms.packets;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public interface FakeEntity {
  int entityId();

  Packet spawn(Location location, org.bukkit.util.Vector velocity);

  default Packet spawn(Location location) {
    return spawn(location, new org.bukkit.util.Vector(0, 0, 0));
  }

  default Packet destroy() {
    return ENTITIES.destroyEntitiesPacket(entityId());
  }

  default Packet teleport(Location location) {
    return ENTITIES.teleportEntityPacket(entityId(), location);
  }

  default Packet ride(int riderId) {
    return ENTITIES.entityMount(riderId, entityId());
  }

  default Packet mount(Entity vehicle) {
    return ENTITIES.entityMount(entityId(), vehicle.getEntityId());
  }

  default Packet wearHead(ItemStack item) {
    return ENTITIES.entityHeadEquipment(entityId(), item);
  }

  default Packet wear(ItemStack head, ItemStack chest, ItemStack legs, ItemStack boots) {
    return ENTITIES.entityEquipment(entityId(), head, chest, legs, boots);
  }

  abstract class Impl implements FakeEntity {
    private final int entityId;

    public Impl(int entityId) {
      this.entityId = entityId;
    }

    @Override
    public int entityId() {
      return entityId;
    }
  }
}
