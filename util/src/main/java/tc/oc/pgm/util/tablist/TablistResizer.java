package tc.oc.pgm.util.tablist;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import java.util.Map;
import tc.oc.pgm.util.nms.packets.PacketEventsUtil;

public class TablistResizer {
  private static final int TAB_SIZE = 80;
  private static PacketListenerCommon listener;

  public static void registerListener() {
    unregisterListener();
    listener = PacketEventsUtil.registerSend(
        PacketListenerPriority.LOWEST,
        Map.of(PacketType.Play.Server.JOIN_GAME, TablistResizer::handleJoinGame));
  }

  public static void unregisterListener() {
    if (listener != null) {
      PacketEventsUtil.unregister(listener);
      listener = null;
    }
  }

  private static void handleJoinGame(PacketSendEvent event) {
    var wrapper = new WrapperPlayServerJoinGame(event);
    wrapper.setMaxPlayers(TAB_SIZE);
    event.markForReEncode(true);
  }
}
