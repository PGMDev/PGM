package tc.oc.pgm.platform.v1_20_6.packets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.packets.Packet;

public class ModernPacket<T extends net.minecraft.network.protocol.Packet<?>>
    implements Packet, PacketSender {

  protected final T packet;

  public ModernPacket(T packet) {
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
