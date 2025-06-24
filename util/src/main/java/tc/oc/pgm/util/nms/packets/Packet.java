package tc.oc.pgm.util.nms.packets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface Packet {

  void send(Player viewer);

  void sendToViewers(Entity entity, boolean excludeSpectators);

  default void broadcast() {
    Bukkit.getOnlinePlayers().forEach(this::send);
  }

  static Packet of(Packet... packets) {
    return switch (packets.length) {
      case 0 -> NoOpPacket.INSTANCE;
      case 1 -> packets[0];
      default -> new CompoundPacket(packets);
    };
  }

  class NoOpPacket implements Packet {
    private static final Packet INSTANCE = new NoOpPacket();

    @Override
    public void send(Player viewer) {}

    @Override
    public void sendToViewers(Entity entity, boolean excludeSpectators) {}
  }

  class CompoundPacket implements Packet {
    private final Packet[] packets;

    public CompoundPacket(Packet... packets) {
      this.packets = packets;
    }

    @Override
    public void send(Player viewer) {
      for (Packet packet : packets) {
        packet.send(viewer);
      }
    }

    @Override
    public void sendToViewers(Entity entity, boolean excludeSpectators) {
      for (Packet packet : packets) {
        packet.sendToViewers(entity, excludeSpectators);
      }
    }
  }
}
