package tc.oc.pgm.util.nms;

import tc.oc.pgm.util.nms.packets.EntityPackets;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.nms.packets.TabPackets;
import tc.oc.pgm.util.platform.Platform;

public interface Packets {
  EntityPackets ENTITIES = Platform.get(EntityPackets.class);
  TabPackets TAB_PACKETS = Platform.get(TabPackets.class);
  PlayerPackets PLAYERS = Platform.get(PlayerPackets.class);
}
