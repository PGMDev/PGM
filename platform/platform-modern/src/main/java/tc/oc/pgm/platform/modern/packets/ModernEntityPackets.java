package tc.oc.pgm.platform.modern.packets;

import static net.minecraft.world.entity.Entity.FLAG_INVISIBLE;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.nms.packets.EntityPackets;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEntityPackets implements EntityPackets {

  private static final EntityDataAccessor<Byte> ENTITY_FLAGS =
      new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
  private static final EntityDataAccessor<Integer> ENTITY_AIR =
      new EntityDataAccessor<>(1, EntityDataSerializers.INT);

  @Override
  public Packet spawnArmorStand(Location loc, int entityId, Vector velocity) {
    return new ModernPacket<>(new ClientboundBundlePacket(List.of(
        new ClientboundAddEntityPacket(
            entityId,
            UUID.randomUUID(),
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            loc.getPitch(),
            loc.getYaw(),
            EntityType.ARMOR_STAND,
            0,
            CraftVector.toNMS(velocity),
            0),
        new ClientboundSetEntityDataPacket(
            entityId,
            List.of(
                SynchedEntityData.DataValue.create(ENTITY_FLAGS, (byte) (1 << FLAG_INVISIBLE)),
                SynchedEntityData.DataValue.create(ENTITY_AIR, 0),
                SynchedEntityData.DataValue.create(ArmorStand.DATA_CLIENT_FLAGS, (byte) 0))))));
  }

  @Override
  public Packet spawnWitherSkull(Location loc, int entityId, Vector velocity) {
    return new ModernPacket<>(new ClientboundAddEntityPacket(
        entityId,
        UUID.randomUUID(),
        loc.getX(),
        loc.getY(),
        loc.getZ(),
        loc.getPitch(),
        loc.getYaw(),
        EntityType.WITHER_SKULL,
        0,
        CraftVector.toNMS(velocity),
        0));
  }

  @Override
  public Packet destroyEntitiesPacket(int... entityIds) {
    return new ModernPacket<>(new ClientboundRemoveEntitiesPacket(entityIds));
  }

  @Override
  public Packet teleportEntityPacket(int entityId, Location location) {
    PacketContainer packet = PlPacket.PL.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

    packet.getIntegers().write(0, entityId);
    packet.getDoubles().write(0, location.getX());
    packet.getDoubles().write(1, location.getY());
    packet.getDoubles().write(2, location.getZ());
    packet.getBytes().write(0, (byte) (location.getYaw() * 256 / 360));
    packet.getBytes().write(1, (byte) (location.getPitch() * 256 / 360));

    return new PlPacket(packet);
  }

  @Override
  public Packet updateHeadRotation(int entityId, Location location) {
    PacketContainer packet = PlPacket.PL.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
    packet.getIntegers().write(0, entityId);
    packet.getBytes().write(0, (byte) (location.getYaw() * 256 / 360));
    return new PlPacket(packet);
  }

  @Override
  public Packet entityMount(int entityId, int vehicleId) {
    PacketContainer packet = PlPacket.PL.createPacket(PacketType.Play.Server.MOUNT);

    packet.getIntegers().write(0, vehicleId);
    packet.getIntegerArrays().write(0, new int[] {entityId});

    return new PlPacket(packet);
  }

  @Override
  public Packet entityEquipment(
      int entityId, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack feet) {
    List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> e = new ArrayList<>();
    if (helmet != null) e.add(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(helmet)));
    if (chest != null) e.add(Pair.of(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(chest)));
    if (legs != null) e.add(Pair.of(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(legs)));
    if (feet != null) e.add(Pair.of(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(feet)));
    return new ModernPacket<>(new ClientboundSetEquipmentPacket(entityId, e));
  }

  @Override
  public Packet entityHeadEquipment(int entityId, ItemStack helmet) {
    var equipment = List.of(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(helmet)));
    return new ModernPacket<>(new ClientboundSetEquipmentPacket(entityId, equipment));
  }

  @Override
  public Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    var data = ((CraftEntity) entity).getHandle().getEntityData().packAll();
    return data == null
        ? Packet.of()
        : new ModernPacket<>(new ClientboundSetEntityDataPacket(entityId, data));
  }
}
