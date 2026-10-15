package tc.oc.pgm.platform.modern.packets;

import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.reflect.ReflectionUtils;

@SuppressWarnings("unchecked")
@Supports(value = PAPER, minVersion = "26.3")
public class ModernPlayerPackets implements PlayerPackets, PacketSender {

  private static final EntityDataAccessor<Pose> DATA_POSE = ReflectionUtils.readStaticField(
      net.minecraft.world.entity.Entity.class, EntityDataAccessor.class, "DATA_POSE");

  private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID =
      ReflectionUtils.readStaticField(
          LivingEntity.class, EntityDataAccessor.class, "SLEEPING_POS_ID");

  @Override
  public void playDeathAnimation(Player player) {
    Location location = player.getLocation();
    BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

    var metadata = new ClientboundSetEntityDataPacket(
        player.getEntityId(),
        List.of(
            SynchedEntityData.DataValue.create(LivingEntity.DATA_HEALTH_ID, 0f),
            SynchedEntityData.DataValue.create(DATA_POSE, Pose.SLEEPING),
            SynchedEntityData.DataValue.create(SLEEPING_POS_ID, Optional.of(pos))));

    sendToViewers(metadata, player, true);
    ENTITIES.teleportEntityPacket(player.getEntityId(), location).sendToViewers(player, false);
  }

  @Override
  public void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningBlocks(show ? Integer.MAX_VALUE : 0);
    send(new ClientboundSetBorderWarningDistancePacket(border), player);
  }

  @Override
  public void fakePlayerItemPickup(Player player, Entity entity) {
    float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, pitch);

    var packet = new ClientboundTakeItemEntityPacket(
        entity.getEntityId(),
        player.getEntityId(),
        entity instanceof Item item ? item.getItemStack().getAmount() : 1);

    sendToViewers(packet, entity, false);
    entity.remove();
  }

  @Override
  public void sendLegacyHelmet(Player player, ItemStack item) {
    var equipment = List.of(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(item)));
    Packet<?> packet = new ClientboundSetEquipmentPacket(player.getEntityId(), equipment);
    for (var viewer : getViewers(player)) {
      if (ViaUtils.getProtocolVersion(viewer.getPlayer().getBukkitEntity()) <= ViaUtils.VERSION_1_7)
        viewer.send(packet);
    }
  }

  @Override
  public void updateVelocity(Player player) {
    var handle = ((CraftPlayer) player).getHandle();
    handle.syncVelocity = false;
    handle.connection.send(new ClientboundSetEntityMotionPacket(handle));
  }
}
