package tc.oc.pgm.platform.modern.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.packets.Packet;

public class PePacket implements Packet, PacketSender {
  private static final PlayerManager PE = PacketEvents.getAPI().getPlayerManager();

  private final PacketWrapper<?> wrapper;

  public PePacket(PacketWrapper<?> wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void send(Player viewer) {
    if (viewer.isOnline()) {
      PE.sendPacket(viewer, wrapper);
    }
  }

  @Override
  public void sendToViewers(Entity entity, boolean excludeSpectators) {
    for (var conn : getViewers(entity)) {
      var viewer = conn.getPlayer().getBukkitEntity();
      if (excludeSpectators) {
        Entity spectatorTarget = viewer.getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      PE.sendPacket(viewer, wrapper);
    }
  }

  @Override
  public void broadcast() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      PE.sendPacket(player, wrapper);
    }
  }
}
