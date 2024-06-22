package tc.oc.pgm.platform.modern.packets;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.nms.packets.Packet;

public class PlPacket implements Packet {
  public static final ProtocolManager PL = ProtocolLibrary.getProtocolManager();

  private final PacketContainer packet;

  public PlPacket(PacketContainer packet) {
    this.packet = packet;
  }

  @Override
  public void send(Player viewer) {
    PL.sendServerPacket(viewer, packet);
  }

  @Override
  public void sendToViewers(Entity entity, boolean excludeSpectators) {
    if (excludeSpectators)
      throw new UnsupportedOperationException(
          "Protocol lib packets can't send to all except spectators");
    PL.broadcastServerPacket(packet, entity, true);
  }

  @Override
  public void broadcast() {
    PL.broadcastServerPacket(packet);
  }
}
