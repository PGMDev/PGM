package tc.oc.pgm.util.nms.packets;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public interface EntityPackets {
  AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);

  default int allocateEntityId() {
    return ENTITY_IDS.decrementAndGet();
  }

  default FakeEntity fakeWitherSkull() {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        return spawnWitherSkull(location, entityId(), velocity);
      }
    };
  }

  default FakeEntity fakeArmorStand(@Nullable ItemStack head) {
    return new FakeEntity.Impl(allocateEntityId()) {
      @Override
      public Packet spawn(Location location, Vector velocity) {
        Packet spawn = spawnArmorStand(location, allocateEntityId(), velocity);
        return head != null ? Packet.of(spawn, wear(4, head)) : spawn;
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

  Packet entityAttach(int entityId, int vehicleId, boolean leash);

  Packet entityEquipment(int entityId, int slot, ItemStack armor);

  Packet entityMetadataPacket(int entityId, Entity entity, boolean complete);
}
