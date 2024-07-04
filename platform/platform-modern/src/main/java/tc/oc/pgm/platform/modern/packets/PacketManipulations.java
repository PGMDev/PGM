package tc.oc.pgm.platform.modern.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PacketManipulations implements Listener {

  private final Int2ReferenceOpenHashMap<Player> playerEntities = new Int2ReferenceOpenHashMap<>();

  public PacketManipulations(Plugin plugin) {
    ProtocolLibrary.getProtocolManager().addPacketListener(new NoSelfDeathListener(plugin));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    playerEntities.put(event.getPlayer().getEntityId(), event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent player) {
    playerEntities.remove(player.getPlayer().getEntityId());
  }

  private class NoSelfDeathListener extends PacketAdapter {

    public NoSelfDeathListener(Plugin plugin) {
      super(
          plugin,
          ListenerPriority.LOWEST,
          PacketType.Play.Server.ENTITY_STATUS,
          PacketType.Play.Server.PLAYER_COMBAT_KILL,
          PacketType.Play.Server.ENTITY_METADATA);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
      var playerId = event.getPlayer().getEntityId();
      var packet = event.getPacket();

      // Strip "Living entity dead" status=3 packets if they're for yourself.
      // This glitches hitboxes
      if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
        int entityId = packet.getIntegers().read(0);
        if (playerId == entityId && packet.getBytes().read(0) == 3) {
          event.setCancelled(true);
        }
      }

      // Never show deaths screens, ever
      if (event.getPacketType() == PacketType.Play.Server.PLAYER_COMBAT_KILL) {
        event.setCancelled(true);
      }

      // Modify "health = 0" metadata packets if they're for yourself.
      // Avoid telling you that you reached 0 health. This freezes you even after respawn.
      if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
        int entityId = packet.getIntegers().read(0);
        if (playerId == entityId) {
          List<WrappedDataValue> read = packet.getDataValueCollectionModifier().read(0);
          for (int i = 0; i < read.size(); i++) {
            var data = read.get(i);
            if (data.getIndex() == LivingEntity.DATA_HEALTH_ID.id()) {
              if ((float) data.getRawValue() <= 0f) {
                // Your own health gone to 0!! nope, take it back
                packet = event.getPacket().deepClone();
                packet.getDataValueCollectionModifier().read(0).get(i).setRawValue(1f);
                event.setPacket(packet);
              }
              break;
            }
          }
        } else {
          Player pl = playerEntities.get(entityId);
          if (pl != null && pl.hasMetadata("isDead")) {
            for (var read : packet.getDataValueCollectionModifier().read(0)) {
              if (read.getIndex() == LivingEntity.DATA_HEALTH_ID.id()) {
                // A dead player's health left 0!! nope, take it back
                if ((float) read.getRawValue() > 0f) {
                  event.setCancelled(true);
                }
                break;
              }
            }
          }
        }
      }
    }
  }
}
