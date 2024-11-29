package tc.oc.pgm.platform.modern.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.util.Packets;
import tc.oc.pgm.platform.modern.util.PlayerTracker;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class PacketManipulations {

  private static final EntityDataAccessor<Float> DATA_HEALTH_ID = LivingEntity.DATA_HEALTH_ID;
  private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES =
      ReflectionUtils.readStaticField(
          LivingEntity.class, EntityDataAccessor.class, "DATA_EFFECT_PARTICLES");

  private final PlayerTracker tracker;

  public PacketManipulations(Plugin plugin, PlayerTracker tracker) {
    this.tracker = tracker;

    Packets.register(
        plugin,
        ListenerPriority.LOWEST,
        Map.of(
            PacketType.Play.Server.ENTITY_STATUS, this::handleEntityStatus,
            PacketType.Play.Server.PLAYER_COMBAT_KILL, this::handleCombatKill,
            PacketType.Play.Server.ENTITY_METADATA, this::handleEntityMetadata));
  }

  private void handleEntityStatus(PacketEvent event) {
    var playerId = event.getPlayer().getEntityId();
    var packet = event.getPacket();

    int entityId = packet.getIntegers().read(0);
    // Strip "Living entity dead" status=3 packets if they're for yourself.
    // This glitches hitboxes
    if (playerId == entityId && packet.getBytes().read(0) == 3) {
      event.setCancelled(true);
    }
  }

  private void handleCombatKill(PacketEvent event) {
    // Never show deaths screens, ever
    event.setCancelled(true);
  }

  private void handleEntityMetadata(PacketEvent event) {
    var playerId = event.getPlayer().getEntityId();
    var packet = event.getPacket();
    int entityId = packet.getIntegers().read(0);
    ClientboundSetEntityDataPacket nmsPacket = (ClientboundSetEntityDataPacket) packet.getHandle();
    var items = nmsPacket.packedItems();

    Player pl = tracker.get(entityId);
    if (pl == null) return;

    boolean isSelf = playerId == entityId;
    boolean isDead = pl.hasMetadata("isDead");
    boolean checkHealth = isSelf || isDead;
    boolean hideParticles = pl.hasMetadata("hideParticles");

    if (!isSelf && !isDead && !hideParticles) return;

    for (int i = 0; i < items.size(); i++) {
      var packedItem = items.get(i);
      if (checkHealth && packedItem.id() == DATA_HEALTH_ID.id()) {
        float val = (float) packedItem.value();
        if (isSelf && val > 0) continue;
        items.set(
            i, SynchedEntityData.DataValue.create(DATA_HEALTH_ID, isSelf ? Math.max(val, 1f) : 0f));
      }

      if (hideParticles && packedItem.id() == DATA_EFFECT_PARTICLES.id()) {
        items.set(i, SynchedEntityData.DataValue.create(DATA_EFFECT_PARTICLES, List.of()));
      }
    }
  }
}
