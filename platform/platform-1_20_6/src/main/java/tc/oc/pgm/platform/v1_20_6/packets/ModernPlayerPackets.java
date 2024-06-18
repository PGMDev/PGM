package tc.oc.pgm.platform.v1_20_6.packets;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernPlayerPackets implements PlayerPackets, PacketSender {

  @Override
  public void playDeathAnimation(Player player) {
    var handle = ((CraftPlayer) player).getHandle();
    var deadStatus = new ClientboundEntityEventPacket(handle, (byte) 3);

    var dataWatcher = ((CraftPlayer) player).getHandle().getEntityData();
    dataWatcher.set(LivingEntity.DATA_HEALTH_ID, 0f, true);
    var noHealth = new ClientboundSetEntityDataPacket(
        player.getEntityId(), Objects.requireNonNull(dataWatcher.packAll()));

    sendToViewers(noHealth, player, true);
    sendToViewers(deadStatus, player, true);
  }

  @Override
  public void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningBlocks(show ? Integer.MAX_VALUE : 0);
    send(new ClientboundSetBorderWarningDistancePacket(border), player);
  }

  @Override
  public void fakePlayerItemPickup(Player player, Item item) {
    float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
    item.getWorld().playSound(item.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, pitch);

    var packet = new ClientboundTakeItemEntityPacket(
        item.getEntityId(), player.getEntityId(), item.getItemStack().getAmount());

    sendToViewers(packet, player, false);
    item.remove();
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
    handle.hurtMarked = false;
    handle.connection.sendPacket(new ClientboundSetEntityMotionPacket(handle));
  }
}
