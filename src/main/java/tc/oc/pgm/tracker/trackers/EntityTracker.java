package tc.oc.pgm.tracker.trackers;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.*;
import tc.oc.util.logging.ClassLogger;

/** Tracks the ownership of {@link Entity}s and resolves damage caused by them */
public class EntityTracker implements Listener {

  private final Logger logger;
  private final Match match;
  private final Map<Entity, TrackerInfo> entities = new HashMap<>();

  public EntityTracker(TrackerMatchModule tmm) {
    this.logger = ClassLogger.get(tmm.getLogger(), getClass());
    this.match = tmm.getMatch();
  }

  public PhysicalInfo createEntity(Entity entity, @Nullable ParticipantState owner) {
    if (entity instanceof ThrownPotion) {
      return new ThrownPotionInfo((ThrownPotion) entity, owner);
    } else if (entity instanceof FallingBlock) {
      return new FallingBlockInfo((FallingBlock) entity, owner);
    } else if (entity instanceof LivingEntity) {
      return new MobInfo((LivingEntity) entity, owner);
    } else {
      return new EntityInfo(entity, owner);
    }
  }

  public PhysicalInfo resolveEntity(Entity entity) {
    MatchPlayer player = match.getParticipant(entity);
    if (player != null) {
      return new PlayerInfo(player);
    }

    TrackerInfo info = entities.get(entity);
    if (info instanceof PhysicalInfo) return (PhysicalInfo) info;

    ParticipantState owner = info instanceof OwnerInfo ? ((OwnerInfo) info).getOwner() : null;
    return createEntity(entity, owner);
  }

  public @Nullable TrackerInfo resolveInfo(Entity entity) {
    return entities.get(checkNotNull(entity));
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

  public void trackEntity(Entity entity, @Nullable TrackerInfo info) {
    checkNotNull(entity);
    if (info == null) {
      entities.remove(entity);
      logger.fine("Clear entity=" + entity);
    } else {
      entities.put(entity, info);
      logger.fine("Track entity=" + entity + " info=" + info);
    }
  }
}
