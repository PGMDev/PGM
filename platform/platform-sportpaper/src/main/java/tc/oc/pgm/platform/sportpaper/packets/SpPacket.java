package tc.oc.pgm.platform.sportpaper.packets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.packets.Packet;

public class SpPacket<T extends net.minecraft.server.v1_8_R3.Packet<?>>
    implements Packet, PacketSender {

  protected final T packet;

  public SpPacket(T packet) {
    this.packet = packet;
  }

  @Override
  public void send(Player viewer) {
    send(packet, viewer);
  }

  @Override
  public void sendToViewers(Entity entity, boolean excludeSpectators) {
    sendToViewers(packet, entity, excludeSpectators);
  }
}
