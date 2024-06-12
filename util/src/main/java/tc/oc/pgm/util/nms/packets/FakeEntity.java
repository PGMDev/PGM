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
    return ENTITIES.entityAttach(riderId, entityId(), false);
  }

  default Packet mount(Entity vehicle) {
    return ENTITIES.entityAttach(entityId(), vehicle.getEntityId(), false);
  }

  default Packet wear(int slot, ItemStack item) {
    return ENTITIES.entityEquipment(entityId(), slot, item);
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
