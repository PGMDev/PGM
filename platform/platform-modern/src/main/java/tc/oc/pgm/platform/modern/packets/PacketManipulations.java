package tc.oc.pgm.platform.modern.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

public class PacketManipulations {
  public static void registerAdapters(Plugin plugin) {
    ProtocolLibrary.getProtocolManager().addPacketListener(new NoSelfDeathListener(plugin));
  }

  private static class NoSelfDeathListener extends PacketAdapter {

    public NoSelfDeathListener(Plugin plugin) {
      super(
          plugin,
          ListenerPriority.LOWEST,
          PacketType.Play.Server.ENTITY_STATUS,
          PacketType.Play.Server.PLAYER_COMBAT_KILL);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
      // Strip "Living entity dead" status packets if they're for yourself.
      // This glitches hitboxes
      if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
        int entityId = event.getPacket().getIntegers().read(0);
        byte status = event.getPacket().getBytes().read(0);
        if (status == 3 && event.getPlayer().getEntityId() == entityId) {
          event.setCancelled(true);
        }
      }

      // Never show deaths screens, ever
      if (event.getPacketType() == PacketType.Play.Server.PLAYER_COMBAT_KILL) {
        event.setCancelled(true);
      }
    }
  }
}
