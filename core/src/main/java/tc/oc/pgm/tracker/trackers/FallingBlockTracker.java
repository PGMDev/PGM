package tc.oc.pgm.tracker.trackers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;
import tc.oc.pgm.tracker.info.FallingBlockInfo;
import tc.oc.pgm.util.event.block.BlockFallEvent;

/** Updates the state of owned falling blocks and entities. */
public class FallingBlockTracker extends AbstractTracker<BlockInfo> implements DamageResolver {

  public FallingBlockTracker(TrackerMatchModule tmm, Match match) {
    super(BlockInfo.class, tmm, match);
  }

  @Nullable
  @Override
  public DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    if (damageType == EntityDamageEvent.DamageCause.FALLING_BLOCK
        && damager instanceof FallingBlockInfo) {
      return (FallingBlockInfo) damager;
    }
    if (damageType == EntityDamageEvent.DamageCause.SUFFOCATION) {
      return blocks().resolveInfo(getEyeLocation(victim).getBlock(), BlockInfo.class);
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlace(ParticipantBlockTransformEvent event) {
    if (event.getNewState().getType().hasGravity()) {
      blocks()
          .trackBlockState(
              event.getNewState(),
              new BlockInfo(event.getNewState().getMaterialData(), event.getPlayerState()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLand(EntityBlockFormEvent event) {
    BlockInfo info = resolveEntity(event.getEntity());
    if (info != null) blocks().trackBlockState(event.getNewState(), info);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onFall(BlockFallEvent event) {
    BlockInfo info = resolveBlock(event.getBlock());
    if (info != null) entities().trackEntity(event.getEntity(), info);
  }

  private Location getEyeLocation(Entity entity) {
    return entity instanceof LivingEntity
        ? ((LivingEntity) entity).getEyeLocation()
        : entity.getLocation();
  }
}
