package tc.oc.pgm.tracker.trackers;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.OwnerInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.TrackerInfo;
import tc.oc.pgm.tracker.info.EntityInfo;
import tc.oc.pgm.tracker.info.FallingBlockInfo;
import tc.oc.pgm.tracker.info.MobInfo;
import tc.oc.pgm.tracker.info.PlayerInfo;
import tc.oc.pgm.tracker.info.ThrownPotionInfo;
import tc.oc.pgm.util.ClassLogger;

/** Tracks the ownership of {@link Entity}s and resolves damage caused by them */
public class EntityTracker implements Listener {

  private final Logger logger;
  private final Match match;
  private final Map<UUID, TrackerInfo> entities = new HashMap<>();

  public EntityTracker(Match match) {
    this.logger = ClassLogger.get(match.getLogger(), getClass());
    this.match = match;
  }

  public PhysicalInfo createEntity(Entity entity, @Nullable ParticipantState owner) {
    return switch (entity) {
      case ThrownPotion thrownPotion -> new ThrownPotionInfo(thrownPotion, owner);
      case FallingBlock fallingBlock -> new FallingBlockInfo(fallingBlock, owner);
      case LivingEntity livingEntity -> new MobInfo(livingEntity, owner);
      default -> new EntityInfo(entity, owner);
    };
  }

  public PhysicalInfo resolveEntity(Entity entity) {
    MatchPlayer player = match.getParticipant(entity);
    if (player != null) {
      return new PlayerInfo(player);
    }

    TrackerInfo info = entities.get(entity.getUniqueId());
    if (info instanceof PhysicalInfo) return (PhysicalInfo) info;

    ParticipantState owner = info instanceof OwnerInfo ? ((OwnerInfo) info).getOwner() : null;
    return createEntity(entity, owner);
  }

  public @Nullable TrackerInfo resolveInfo(Entity entity) {
    return entities.get(entity.getUniqueId());
  }

  public @Nullable <T extends TrackerInfo> T resolveInfo(Entity entity, Class<T> infoType) {
    TrackerInfo info = resolveInfo(entity);
    return infoType.isInstance(info) ? infoType.cast(info) : null;
  }

  public @Nullable ParticipantState getOwner(Entity entity) {
    if (entity instanceof Player) {
      return match.getParticipantState(entity); // Players own themselves
    } else {
      OwnerInfo info = resolveInfo(entity, OwnerInfo.class);
      return info == null ? null : info.getOwner();
    }
  }

  public void trackEntity(Entity entity, TrackerInfo info) {
    entities.put(entity.getUniqueId(), info);
    logger.fine("Track entity=" + entity + " info=" + info);
  }

  public void untrackEntity(Entity entity) {
    entities.remove(entity.getUniqueId());
    logger.fine("Clear entity=" + entity);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityRemove(EntityRemoveFromWorldEvent event) {
    if (!MISC_UTILS.isEntityDestroyed(event)) return;
    Entity entity = event.getEntity();
    if (!entities.containsKey(entity.getUniqueId())) return;

    // Deferred so anything resolving the entity later this tick still finds it
    match.getExecutor(MatchScope.RUNNING).execute(() -> untrackEntity(entity));
  }
}
