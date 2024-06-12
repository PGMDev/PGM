package tc.oc.pgm.platform.sportpaper.packets;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTrackerEntry;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface PacketSender {
  default void send(Packet<?> packet, Player viewer) {
    if (viewer.isOnline()) {
      EntityPlayer nmsPlayer = ((CraftPlayer) viewer).getHandle();
      nmsPlayer.playerConnection.sendPacket(packet);
    }
  }

  default void sendToViewers(Packet<?> packet, Entity entity, boolean excludeSpectators) {
    for (EntityPlayer viewer : getViewers(entity)) {
      if (excludeSpectators) {
        Entity spectatorTarget = viewer.getBukkitEntity().getSpectatorTarget();
        if (spectatorTarget != null && spectatorTarget.getUniqueId().equals(entity.getUniqueId()))
          continue;
      }
      viewer.playerConnection.sendPacket(packet);
    }
  }

  default Iterable<EntityPlayer> getViewers(Entity entity) {
    net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) entity).getHandle();
    EntityTrackerEntry entry =
        ((WorldServer) nms.getWorld()).getTracker().trackedEntities.get(nms.getId());
    return entry.trackedPlayers;
  }
}
