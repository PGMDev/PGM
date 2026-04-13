package tc.oc.pgm.platform.modern.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.packets.Packet;

public class PePacket implements Packet, PacketSender {
  private final PacketWrapper<?> wrapper;

  public PePacket(PacketWrapper<?> wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void send(Player viewer) {
    if (viewer.isOnline()) {
      PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, wrapper);
    }
  }

  @Override
  public void sendToViewers(Entity entity, boolean excludeSpectators) {
    for (var conn : getViewers(entity)) {
      if (excludeSpectators) {
        Entity spectatorTarget = conn.getPlayer().getBukkitEntity().getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      PacketEvents.getAPI()
          .getPlayerManager()
          .sendPacket(conn.getPlayer().getBukkitEntity(), wrapper);
    }
  }

  @Override
  public void broadcast() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }
  }
}
