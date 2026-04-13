package tc.oc.pgm.platform.modern.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;

public class Packets {
  private static final Consumer<PacketSendEvent> SEND_NO_OP = event -> {};
  private static final Consumer<PacketReceiveEvent> RECEIVE_NO_OP = event -> {};

  public static PacketListenerCommon registerSend(
      PacketListenerPriority priority, Map<PacketTypeCommon, Consumer<PacketSendEvent>> handlers) {
    return PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListenerAbstract(priority) {
          @Override
          public void onPacketSend(@NonNull PacketSendEvent event) {
            handlers.getOrDefault(event.getPacketType(), SEND_NO_OP).accept(event);
          }
        });
  }

  public static PacketListenerCommon registerReceive(
      PacketListenerPriority priority,
      Map<PacketTypeCommon, Consumer<PacketReceiveEvent>> handlers) {
    return PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListenerAbstract(priority) {
          @Override
          public void onPacketReceive(@NonNull PacketReceiveEvent event) {
            handlers.getOrDefault(event.getPacketType(), RECEIVE_NO_OP).accept(event);
          }
        });
  }

  public static void unregister(PacketListenerCommon listener) {
    PacketEvents.getAPI().getEventManager().unregisterListener(listener);
  }
}
