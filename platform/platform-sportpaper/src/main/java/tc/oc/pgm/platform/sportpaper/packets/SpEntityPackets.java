package tc.oc.pgm.platform.sportpaper.packets;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PacketPlayOutAttachEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.util.nms.packets.EntityPackets;
import tc.oc.pgm.util.nms.packets.Packet;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpEntityPackets implements EntityPackets {

  private static final int WITHER_SKULL = 66;

  @Override
  public Packet spawnArmorStand(Location loc, int entityId, Vector velocity) {
    DataWatcher dataWatcher = new DataWatcher(null);
    // https://wiki.vg/index.php?title=Entity_metadata&oldid=7415
    dataWatcher.a(0, (byte) 0x20); // 0 = Flags, 0x20 = invisible
    dataWatcher.a(1, (short) 0); // 1 = air, 0
    dataWatcher.a(10, (byte) 0); // 10 = armor stand flags, 0 = no gravity, nor arms, etc
    return new SpPacket<>(new PacketPlayOutSpawnEntityLiving(
        entityId,
        (byte) EntityType.ARMOR_STAND.getTypeId(),
        loc.getX(),
        loc.getY(),
        loc.getZ(),
        loc.getYaw(),
        loc.getPitch(),
        loc.getPitch(),
        (int) (velocity.getX() * 8000),
        (int) (velocity.getY() * 8000),
        (int) (velocity.getZ() * 8000),
        dataWatcher));
  }

  @Override
  public Packet spawnWitherSkull(Location loc, int entityId, Vector velocity) {
    return new SpPacket<>(new PacketPlayOutSpawnEntity(
        entityId,
        loc.getX(),
        loc.getY(),
        loc.getZ(),
        (int) (velocity.getX() * 8000),
        (int) (velocity.getY() * 8000),
        (int) (velocity.getZ() * 8000),
        (int) loc.getPitch(),
        (int) loc.getYaw(),
        WITHER_SKULL,
        0));
  }

  @Override
  public Packet destroyEntitiesPacket(int... entityIds) {
    return new SpPacket<>(new PacketPlayOutEntityDestroy(entityIds));
  }

  @Override
  public Packet teleportEntityPacket(int entityId, Location location) {
    return new SpPacket<>(new PacketPlayOutEntityTeleport(
        entityId, // Entity ID
        (int) (location.getX() * 32), // World X * 32
        (int) (location.getY() * 32), // World Y * 32
        (int) (location.getZ() * 32), // World Z * 32
        (byte) (location.getYaw() * 256 / 360), // Yaw
        (byte) (location.getPitch() * 256 / 360), // Pitch
        true)); // On Ground + Height Correction
  }

  private static final EntityMock ENTITY_MOCK = new EntityMock();

  @Override
  public Packet updateHeadRotation(int entityId, Location location) {
    return new SpPacket<>(new PacketPlayOutEntityHeadRotation(
        ENTITY_MOCK.withId(entityId), (byte) (location.getYaw() * 256.0F / 360.0F)));
  }

  @Override
  public Packet entityMount(int entityId, int vehicleId) {
    return new SpPacket<>(new PacketPlayOutAttachEntity(entityId, vehicleId, false));
  }

  @Override
  public Packet entityEquipment(
      int entityId, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack feet) {
    List<Packet> packets = new ArrayList<>();
    if (helmet != null) packets.add(makeEquipment(entityId, EquipmentSlot.HEAD, helmet));
    if (chest != null) packets.add(makeEquipment(entityId, EquipmentSlot.CHEST, chest));
    if (legs != null) packets.add(makeEquipment(entityId, EquipmentSlot.LEGS, legs));
    if (feet != null) packets.add(makeEquipment(entityId, EquipmentSlot.FEET, feet));
    return Packet.of(packets.toArray(Packet[]::new));
  }

  @Override
  public Packet entityHeadEquipment(int entityId, ItemStack helmet) {
    return makeEquipment(entityId, EquipmentSlot.HEAD, helmet);
  }

  private Packet makeEquipment(int entityId, EquipmentSlot slot, ItemStack item) {
    return new SpPacket<>(
        new PacketPlayOutEntityEquipment(entityId, slot.ordinal(), CraftItemStack.asNMSCopy(item)));
  }

  @Override
  public Packet entityMetadataPacket(int entityId, Entity entity, boolean complete) {
    return new SpPacket<>(new PacketPlayOutEntityMetadata(
        entityId,
        ((CraftEntity) entity).getHandle().getDataWatcher(),
        complete)); // true = all values, false = only dirty values
  }

  private static class EntityMock extends net.minecraft.server.v1_8_R3.Entity {
    private int id;

    public EntityMock() {
      super(null);
    }

    @Override
    public int getId() {
      return id;
    }

    public EntityMock withId(int id) {
      this.id = id;
      return this;
    }

    @Override
    protected void h() {}

    @Override
    protected void a(NBTTagCompound nbtTagCompound) {}

    @Override
    protected void b(NBTTagCompound nbtTagCompound) {}
  }
}
