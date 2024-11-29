package tc.oc.pgm.platform.modern.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

public class Packets {
  private static final Consumer<PacketEvent> NO_OP = event -> {};

  public static void register(
      Plugin pl, ListenerPriority priority, Map<PacketType, Consumer<PacketEvent>> events) {
    ProtocolLibrary.getProtocolManager()
        .addPacketListener(new PacketAdapter(pl, priority, events.keySet()) {
          @Override
          public void onPacketReceiving(PacketEvent event) {
            events.getOrDefault(event.getPacketType(), NO_OP).accept(event);
          }

          @Override
          public void onPacketSending(PacketEvent event) {
            events.getOrDefault(event.getPacketType(), NO_OP).accept(event);
          }
        });
  }
}
