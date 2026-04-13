package tc.oc.pgm.util.tablist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import org.jspecify.annotations.NonNull;

public class TablistResizer {
  private static final int TAB_SIZE = 80;
  private static PacketListenerCommon listener;

  public static void registerListener() {
    unregisterListener();
    listener = PacketEvents.getAPI()
        .getEventManager()
        .registerListener(new PacketListenerAbstract(PacketListenerPriority.LOWEST) {
          @Override
          public void onPacketSend(@NonNull PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.JOIN_GAME) return;
            var wrapper = new WrapperPlayServerJoinGame(event);
            wrapper.setMaxPlayers(TAB_SIZE);
            event.markForReEncode(true);
          }
        });
  }

  public static void unregisterListener() {
    if (listener != null) {
      PacketEvents.getAPI().getEventManager().unregisterListener(listener);
      listener = null;
    }
  }
}
