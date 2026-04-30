package tc.oc.pgm.platform.modern.packets;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.listeners.PlayerTracker;
import tc.oc.pgm.util.event.ExtraPingDataRequestEvent;
import tc.oc.pgm.util.packets.PacketEventsUtil;
import tc.oc.pgm.util.reflect.ReflectionUtils;

@SuppressWarnings("unchecked")
public class PacketManipulations {

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
  private final PacketListenerCommon[] listeners;

  public PacketManipulations(PlayerTracker tracker) {
    this.tracker = tracker;

    this.listeners = new PacketListenerCommon[] {
      PacketEventsUtil.registerSend(
          PacketListenerPriority.LOWEST,
          Map.of(
              PacketType.Play.Server.ENTITY_STATUS, this::handleEntityStatus,
              PacketType.Play.Server.DEATH_COMBAT_EVENT, this::handleDeathCombatEvent,
              PacketType.Play.Server.ENTITY_METADATA, this::handleEntityMetadata)),
      PacketEventsUtil.registerSend(
          PacketListenerPriority.HIGHEST,
          Map.of(PacketType.Status.Server.RESPONSE, this::handleServerPing))
    };
  }

  public void unregister() {
    for (PacketListenerCommon listener : listeners) {
      PacketEventsUtil.unregister(listener);
    }
  }

  private void handleEntityStatus(PacketSendEvent event) {
    var wrapper = new WrapperPlayServerEntityStatus(event);
    Player player = event.getPlayer();
    // Strip "Living entity dead" status=3 packets if they're for yourself.
    // This glitches hitboxes
    if (player.getEntityId() == wrapper.getEntityId() && wrapper.getStatus() == 3) {
      event.setCancelled(true);
    }
  }

  private void handleDeathCombatEvent(PacketSendEvent event) {
    // Never show death screens, ever
    event.setCancelled(true);
  }

  private void handleEntityMetadata(PacketSendEvent event) {
    var wrapper = new WrapperPlayServerEntityMetadata(event);
    int entityId = wrapper.getEntityId();
    var items = wrapper.getEntityMetadata();

    Player pl = tracker.get(entityId);
    // We're only interested in modifying players
    if (pl == null) return;

    Player receiver = event.getPlayer();
    boolean modified = false;

    // Strip the invisibility flag for players who can see invisible teammates
    if (pl.isInvisible() && receiver.hasMetadata(SHOW_INVISIBLE_KEY)) {
      for (int i = 0; i < items.size(); i++) {
        var item = items.get(i);
        if (item.getIndex() == DATA_SHARED_FLAGS_ID.id()) {
          byte val = (Byte) item.getValue();
          if ((val & INVISIBILITY) != 0) {
            items.set(i, new EntityData<>(item.getIndex(), EntityDataTypes.BYTE, (byte)
                (val & ~INVISIBILITY)));
            modified = true;
            break;
          }
        }
      }
    }

    boolean isSelf = receiver.getEntityId() == entityId;
    boolean isDead = pl.hasMetadata("isDead");
    boolean checkHealth = isSelf || isDead;
    boolean hideParticles = pl.hasMetadata(HIDE_PARTICLES_KEY);

    if (!checkHealth && !hideParticles) {
      if (modified) event.markForReEncode(true);
      return;
    }

    for (EntityData<?> item : items) {
      if (checkHealth && item.getIndex() == DATA_HEALTH_ID.id()) {
        float val = (Float) item.getValue();
        if (isSelf ? val <= 0 : val > 0) {
          ((EntityData<Float>) item).setValue(isSelf ? Math.max(val, 1f) : 0f);
          modified = true;
        }
        checkHealth = false;
      }

      if (hideParticles && item.getIndex() == DATA_EFFECT_PARTICLES.id()) {
        List<?> val = (List<?>) item.getValue();
        if (!val.isEmpty()) {
          ((EntityData<List<?>>) item).setValue(List.of());
          modified = true;
        }
        hideParticles = false;
      }

      if (!checkHealth && !hideParticles) break;
    }

    if (modified) event.markForReEncode(true);
  }

  private void handleServerPing(PacketSendEvent event) {
    if (event.isCancelled()) return;
    JsonObject pingExtra = new JsonObject();
    new ExtraPingDataRequestEvent() {
      @Override
      public JsonObject getServerListExtra(Plugin plugin) {
        return (JsonObject)
            pingExtra.asMap().computeIfAbsent(plugin.namespace(), k -> new JsonObject());
      }
    }.callEvent();

    if (!pingExtra.isEmpty()) {
      var wrapper = new WrapperStatusServerResponse(event);
      JsonObject jsonData = wrapper.getComponent();
      jsonData.add("bukkit_extra", pingExtra);
      wrapper.setComponent(jsonData);
      event.markForReEncode(true);
    }
  }
}
