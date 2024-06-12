package tc.oc.pgm.util.nms;

import tc.oc.pgm.util.nms.packets.EntityPackets;
import tc.oc.pgm.util.nms.packets.PlayerPackets;
import tc.oc.pgm.util.nms.packets.TabPackets;
import tc.oc.pgm.util.platform.Platform;

public interface Packets {
  EntityPackets ENTITIES = Platform.requireInstance(EntityPackets.class);
  TabPackets TAB_PACKETS = Platform.requireInstance(TabPackets.class);
  PlayerPackets PLAYERS = Platform.requireInstance(PlayerPackets.class);
}
