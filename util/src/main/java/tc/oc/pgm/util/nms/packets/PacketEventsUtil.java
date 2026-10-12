package tc.oc.pgm.util.nms.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PacketEventsUtil {
  private static final Consumer<PacketSendEvent> SEND_NO_OP = event -> {};
  private static final Consumer<PacketReceiveEvent> RECEIVE_NO_OP = event -> {};

  public static PacketListenerCommon registerSend(
      PacketListenerPriority priority, Map<PacketTypeCommon, Consumer<PacketSendEvent>> handlers) {
    return eventManager().registerListener(new PacketListenerAbstract(priority) {
      @Override
      public void onPacketSend(PacketSendEvent event) {
        handlers.getOrDefault(event.getPacketType(), SEND_NO_OP).accept(event);
      }
    });
  }

  public static PacketListenerCommon registerReceive(
      PacketListenerPriority priority,
      Map<PacketTypeCommon, Consumer<PacketReceiveEvent>> handlers) {
    return eventManager().registerListener(new PacketListenerAbstract(priority) {
      @Override
      public void onPacketReceive(PacketReceiveEvent event) {
        handlers.getOrDefault(event.getPacketType(), RECEIVE_NO_OP).accept(event);
      }
    });
  }

  public static void unregister(PacketListenerCommon listener) {
    eventManager().unregisterListener(listener);
  }

  public static <E extends ProtocolPacketEvent> void reEncodeIfModified(
      E event, Predicate<? super E> rewrite) {
    var previousWrapper = event.getLastUsedWrapper();
    if (rewrite.test(event)) event.markForReEncode(true);
    else event.setLastUsedWrapper(previousWrapper);
  }

  private static EventManager eventManager() {
    return PacketEvents.getAPI().getEventManager();
  }
}
