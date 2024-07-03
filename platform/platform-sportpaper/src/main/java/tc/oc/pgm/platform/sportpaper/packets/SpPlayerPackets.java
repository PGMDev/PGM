package tc.oc.pgm.platform.sportpaper.packets;

import static net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder.EnumWorldBorderAction.SET_WARNING_BLOCKS;
import static tc.oc.pgm.util.nms.Packets.ENTITIES;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.Collections;
import java.util.List;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutBed;
import net.minecraft.server.v1_8_R3.PacketPlayOutCollect;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_8_R3.WorldBorder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpPlayerPackets implements PlayerPackets, PacketSender {

  private static final int TYPE_FLOAT = 3;
  private static final int HEALTH_FIELD = 6;

  @Override
  public void playDeathAnimation(Player player) {
    EntityPlayer handle = ((CraftPlayer) player).getHandle();
    PacketPlayOutEntityMetadata noHealthMeta =
        new PacketPlayOutEntityMetadata(handle.getId(), handle.getDataWatcher(), false);

    // Add/replace health to zero
    DataWatcher.WatchableObject zeroHealth =
        new DataWatcher.WatchableObject(TYPE_FLOAT, HEALTH_FIELD, 0f);

    List<DataWatcher.WatchableObject> list = noHealthMeta.b;
    if (noHealthMeta.b == null) {
      noHealthMeta.b = Collections.singletonList(zeroHealth);
    } else {
      boolean replaced = false;
      for (int i = 0; i < list.size(); i++) {
        DataWatcher.WatchableObject wo = list.get(i);
        if (wo.a() == HEALTH_FIELD) {
          list.set(i, zeroHealth);
          replaced = true;
          break;
        }
      }
      if (!replaced) list.add(zeroHealth);
    }
    Location location = player.getLocation();
    BlockPosition pos = new BlockPosition(location.getX(), location.getY(), location.getZ());

    sendToViewers(noHealthMeta, player, true);
    sendToViewers(new PacketPlayOutBed(handle, pos), player, true);
    ENTITIES.teleportEntityPacket(player.getEntityId(), location).sendToViewers(player, true);
  }

  @Override
  public void showBorderWarning(Player player, boolean show) {
    WorldBorder border = new WorldBorder();
    border.setWarningDistance(show ? Integer.MAX_VALUE : 0);
    send(new PacketPlayOutWorldBorder(border, SET_WARNING_BLOCKS), player);
  }

  @Override
  public void fakePlayerItemPickup(Player player, Item item) {
    float pitch = (((float) (Math.random() - Math.random()) * 0.7F + 1.0F) * 2.0F);
    item.getWorld().playSound(item.getLocation(), org.bukkit.Sound.ITEM_PICKUP, 0.2F, pitch);
    sendToViewers(new PacketPlayOutCollect(item.getEntityId(), player.getEntityId()), item, false);
    item.remove();
  }

  @Override
  public void sendLegacyHelmet(Player player, ItemStack item) {
    var packet = ENTITIES.entityHeadEquipment(player.getEntityId(), item);
    for (EntityPlayer viewer : getViewers(player)) {
      if (ViaUtils.getProtocolVersion(viewer.getBukkitEntity()) <= ViaUtils.VERSION_1_7)
        packet.send(viewer.getBukkitEntity());
    }
  }

  @Override
  public void updateVelocity(Player player) {
    EntityPlayer handle = ((CraftPlayer) player).getHandle();
    handle.velocityChanged = false;
    handle.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(handle));
  }
}
