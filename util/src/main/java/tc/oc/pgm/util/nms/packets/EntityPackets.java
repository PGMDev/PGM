package tc.oc.pgm.util.nms.packets;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public interface EntityPackets {

  default int allocateEntityId() {
    return NMS_HACKS.allocateEntityId();
  }

  default FakeEntity fakeWitherSkull() {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        return spawnWitherSkull(location, entityId(), velocity);
      }
    };
  }

  default FakeEntity fakeArmorStand(@Nullable ItemStack helmet) {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        Packet spawn = spawnArmorStand(location, entityId(), velocity);
        return helmet != null ? Packet.of(spawn, wearHead(helmet)) : spawn;
      }
    };
  }

  default Packet spawnFreezeEntity(Player player, int entityId, boolean legacy) {
    if (legacy) {
      Location loc = player.getLocation().add(0, 0.286, 0);
      if (loc.getY() < -64) {
        loc.setY(-64);
        player.teleport(loc);
      }
      return spawnWitherSkull(loc, entityId, new Vector());
    } else {
      Location loc = player.getLocation().subtract(0, 1.1, 0);
      return spawnArmorStand(loc, entityId, new Vector());
    }
  }

  Packet spawnArmorStand(Location loc, int entityId, Vector velocity);

  Packet spawnWitherSkull(Location location, int entityId, Vector velocity);

  Packet destroyEntitiesPacket(int... entityIds);

  Packet teleportEntityPacket(int entityId, Location location);

  Packet entityMount(int entityId, int vehicleId);

  Packet entityHeadEquipment(int entityId, ItemStack helmet);

  Packet entityMetadataPacket(int entityId, Entity entity, boolean complete);
}
