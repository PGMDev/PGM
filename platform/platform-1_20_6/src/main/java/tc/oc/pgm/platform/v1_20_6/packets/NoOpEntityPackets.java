package tc.oc.pgm.platform.v1_20_6.packets;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.nms.packets.EntityPackets;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class NoOpEntityPackets implements EntityPackets {

  @Override
  public Packet spawnArmorStand(Location loc, int entityId, Vector velocity) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet spawnWitherSkull(Location location, int entityId, Vector velocity) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet destroyEntitiesPacket(int... entityIds) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet teleportEntityPacket(int entityId, Location location) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet entityAttach(int entityId, int vehicleId, boolean leash) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet entityEquipment(int entityId, int slot, ItemStack armor) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }

  @Override
  public Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return new Packet() {
      @Override
      public void send(Player viewer) {}

      @Override
      public void sendToViewers(Entity entity, boolean excludeSpectators) {}
    };
  }
}
