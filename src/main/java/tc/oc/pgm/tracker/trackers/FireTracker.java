package tc.oc.pgm.tracker.trackers;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExtinguishEvent;
import tc.oc.material.Materials;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.BlockInfo;
import tc.oc.pgm.tracker.damage.EntityInfo;
import tc.oc.pgm.tracker.damage.FireInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.resolvers.DamageResolver;

/**
 * - Updates the state of owned fire and lava blocks from events - Tracks burning entities that were
 * ignited by players, directly or indirectly - Resolves fire tick damage to those entities
 */
public class FireTracker extends AbstractTracker<FireInfo> implements DamageResolver {

  // An entity can be owned by one player but ignited by another, so we need an independent map for
  // burning
  private final Map<Entity, FireInfo> burningEntities = new WeakHashMap<>();

  public FireTracker(TrackerMatchModule tmm, Match match) {
    super(FireInfo.class, tmm, match);
  }

  @Override
  public @Nullable FireInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    switch (damageType) {
      case FIRE_TICK:
        FireInfo info = resolveBurning(victim);
        if (info != null) return info;
        // fall through

      case FIRE:
      case LAVA:
        return new FireInfo(damager);
    }
    return null;
  }

  public @Nullable FireInfo resolveBurning(Entity entity) {
    return burningEntities.get(entity);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockTransform(BlockTransformEvent event) {
    boolean wasLava = Materials.isLava(event.getOldState());
    boolean isLava = Materials.isLava(event.getNewState());

    if (event.changedFrom(Material.FIRE) || (wasLava && !isLava)) {
      blocks().clearBlock(event.getBlock());
    }
    if (event instanceof ParticipantBlockTransformEvent
        && (event.changedTo(Material.FIRE) || (!wasLava && isLava))) {
      ParticipantState placer = ((ParticipantBlockTransformEvent) event).getPlayerState();
      blocks()
          .trackBlockState(
              event.getNewState(), new FireInfo(new BlockInfo(event.getNewState(), placer)));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityIgnite(EntityCombustByBlockEvent event) {
    if (event.getDuration() == 0) return;

    ParticipantState owner = blocks().getOwner(event.getCombuster());
    if (owner != null) {
      burningEntities.put(
          event.getEntity(), new FireInfo(blocks().resolveBlock(event.getCombuster())));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityIgnite(EntityCombustByEntityEvent event) {
    if (event.getDuration() == 0) return;

    FireInfo info = resolveBurning(event.getCombuster());
    if (info != null) {
      // First, try to resolve the player who ignited the combuster
      info = new FireInfo(new EntityInfo(event.getCombuster(), info.getOwner()));
    } else {
      // If an igniter is not found, fall back to the owner of the entity
      info = new FireInfo(entities().resolveEntity(event.getCombuster()));
    }

    burningEntities.put(event.getEntity(), info);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExtinguish(EntityExtinguishEvent event) {
    this.burningEntities.remove(event.getEntity());
  }
}
