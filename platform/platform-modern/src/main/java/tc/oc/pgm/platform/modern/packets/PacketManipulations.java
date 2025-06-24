package tc.oc.pgm.platform.modern.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.listeners.PlayerTracker;
import tc.oc.pgm.platform.modern.util.Packets;
import tc.oc.pgm.util.reflect.ReflectionUtils;

public class PacketManipulations implements PacketSender {

  public static final String HIDE_PARTICLES_KEY = "hideParticles";
  public static final String SHOW_INVISIBLE_KEY = "showInvisible";

  protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID =
      ReflectionUtils.readStaticField(
          Entity.class, EntityDataAccessor.class, "DATA_SHARED_FLAGS_ID");
  private static final EntityDataAccessor<Float> DATA_HEALTH_ID = LivingEntity.DATA_HEALTH_ID;
  private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES =
      ReflectionUtils.readStaticField(
          LivingEntity.class, EntityDataAccessor.class, "DATA_EFFECT_PARTICLES");

  private static final int INVISIBILITY = 0x20;

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
    ClientboundSetEntityDataPacket nmsPacket =
        (ClientboundSetEntityDataPacket) event.getPacket().getHandle();
    int entityId = nmsPacket.id();
    var items = nmsPacket.packedItems();

    Player pl = tracker.get(entityId);
    // We're only interested in modifying players
    if (pl == null) return;

    // Replace the packet without invisibility if needed
    if (pl.isInvisible() && event.getPlayer().hasMetadata(SHOW_INVISIBLE_KEY)) {
      for (int i = 0; i < items.size(); i++) {
        var item = items.get(i);
        if (item.id() == DATA_SHARED_FLAGS_ID.id()) {
          byte val = (byte) item.value();
          if ((val & INVISIBILITY) != 0) {
            // Because multiple players could receive this packet, we can't mutate it.
            // Make a clone to send instead, and cancel the original
            var newItems = new ArrayList<>(items);
            newItems.set(i, SynchedEntityData.DataValue.create(DATA_SHARED_FLAGS_ID, (byte)
                (val & ~INVISIBILITY)));

            send(new ClientboundSetEntityDataPacket(entityId, newItems), event.getPlayer());
            event.setCancelled(true);
            return;
          }
        }
      }
    }

    boolean isSelf = event.getPlayer().getEntityId() == entityId;
    boolean isDead = pl.hasMetadata("isDead");
    boolean checkHealth = isSelf || isDead;
    boolean hideParticles = pl.hasMetadata(HIDE_PARTICLES_KEY);

    // Nothing to check, move along
    if (!checkHealth && !hideParticles) return;

    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);

      if (checkHealth && item.id() == DATA_HEALTH_ID.id()) {
        float val = (float) item.value();
        if (isSelf ? val <= 0 : val > 0) {
          val = isSelf ? Math.max(val, 1f) : 0f;
          items.set(i, SynchedEntityData.DataValue.create(DATA_HEALTH_ID, val));
        }
        checkHealth = false;
      }

      if (hideParticles && item.id() == DATA_EFFECT_PARTICLES.id()) {
        List<?> val = (List<?>) item.value();
        if (!val.isEmpty()) {
          items.set(i, SynchedEntityData.DataValue.create(DATA_EFFECT_PARTICLES, List.of()));
        }
        hideParticles = false;
      }

      if (!checkHealth && !hideParticles) return;
    }
  }
}
