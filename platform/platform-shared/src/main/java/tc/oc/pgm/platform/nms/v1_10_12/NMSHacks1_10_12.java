package tc.oc.pgm.platform.nms.v1_10_12;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPIGOT;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.platform.nms.v1_9.NMSHacks1_9;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPIGOT, minVersion = "1.10", maxVersion = "1.12.2")
@Supports(value = PAPER, minVersion = "1.10", maxVersion = "1.12.2")
public class NMSHacks1_10_12 extends NMSHacks1_9 {
  @Override
  public void clearArrowsInPlayer(Player player) {
    WrappedDataWatcher entityWatcher = WrappedDataWatcher.getEntityWatcher(player);
    entityWatcher.setObject(10, (int) 0, true);
  }

  @Override
  public @NotNull PacketContainer getMetadataPacket(Player player, float health) {
    PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

    metadataPacket.getIntegers().write(0, player.getEntityId());

    WrappedDataWatcher wrappedDataWatcher = WrappedDataWatcher.getEntityWatcher(player).deepClone();

    WrappedDataWatcher.WrappedDataWatcherObject watcherObject =
        new WrappedDataWatcher.WrappedDataWatcherObject(
            7, WrappedDataWatcher.Registry.get(Float.class));
    wrappedDataWatcher.setObject(watcherObject, health);

    metadataPacket
        .getWatchableCollectionModifier()
        .write(0, wrappedDataWatcher.getWatchableObjects());
    return metadataPacket;
  }

  @Override
  public void setPotionParticles(Player player, boolean enabled) {
    WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(player);

    if (enabled) {
      Collection<PotionEffect> activePotionEffects = player.getActivePotionEffects();
      for (PotionEffect potionEffect : activePotionEffects) {
        if (!potionEffect.isAmbient()) {
          dataWatcher.setObject(9, false, true);
          dataWatcher.setObject(8, potionEffect.getType().getId(), true);
        }
      }
    }
    dataWatcher.setObject(8, (int) 0, true);
    dataWatcher.setObject(9, true, true);
  }

  @Override
  public void spawnFakeArmorStand(Player player, int entityId, Location location, Vector velocity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
    packet
        .getIntegers()
        .write(0, entityId)
        .write(1, 30) // armor stand
        .write(2, (int) (velocity.getX() * 8000))
        .write(3, (int) (velocity.getY() * 8000))
        .write(4, (int) (velocity.getZ() * 8000));
    packet
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());

    packet
        .getBytes()
        .write(0, (byte) (int) (location.getYaw() * 256.0F / 360.0F))
        .write(1, (byte) (int) (location.getPitch() * 256.0F / 360.0F))
        .write(2, (byte) (int) (location.getPitch() * 256.0F / 360.0F));

    packet.getUUIDs().write(0, UUID.randomUUID());

    WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            0, WrappedDataWatcher.Registry.get(Byte.class)),
        (byte) 0x20);
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            4, WrappedDataWatcher.Registry.get(Boolean.class)),
        true);
    dataWatcher.setObject(
        new WrappedDataWatcher.WrappedDataWatcherObject(
            10, WrappedDataWatcher.Registry.get(Byte.class)),
        (byte) 0x8);

    packet.getDataWatcherModifier().write(0, dataWatcher);

    sendPacket(player, packet);
  }
}
