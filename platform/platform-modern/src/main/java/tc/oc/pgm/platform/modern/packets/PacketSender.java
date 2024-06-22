package tc.oc.pgm.platform.modern.packets;

import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerPlayerConnection;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface PacketSender {
  default void send(Packet<?> packet, Player viewer) {
    if (viewer.isOnline()) {
      var nmsPlayer = ((CraftPlayer) viewer).getHandle();
      nmsPlayer.connection.sendPacket(packet);
    }
  }

  default void sendToViewers(Packet<?> packet, Entity entity, boolean excludeSpectators) {
    for (var conn : getViewers(entity)) {
      if (excludeSpectators) {
        Entity spectatorTarget = conn.getPlayer().getBukkitEntity().getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      conn.send(packet);
    }
  }

  default Iterable<ServerPlayerConnection> getViewers(Entity entity) {
    ServerLevel world = (ServerLevel) ((CraftEntity) entity).getHandle().level();
    var entityTracker = world.getChunkSource().chunkMap.entityMap.get(entity.getEntityId());
    return entityTracker == null ? List.of() : entityTracker.seenBy;
  }
}
