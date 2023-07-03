package tc.oc.pgm.util.tablist;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

public class TablistResizer {
  private static final int TAB_SIZE = 80;

  public static void registerAdapter(Plugin plugin) {
    ProtocolLibrary.getProtocolManager().addPacketListener(new TablistResizePacketAdapter(plugin));
  }

  private static class TablistResizePacketAdapter extends PacketAdapter {

    public TablistResizePacketAdapter(Plugin plugin) {
      super(plugin, ListenerPriority.LOWEST, PacketType.Play.Server.LOGIN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
      if (event.getPacketType() == PacketType.Play.Server.LOGIN)
        event.getPacket().getIntegers().write(2, TAB_SIZE);
    }
  }
}
