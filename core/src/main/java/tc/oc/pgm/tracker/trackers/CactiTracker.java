package tc.oc.pgm.tracker.trackers;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.BlockInfo;

public class CactiTracker extends AbstractTracker<BlockInfo> implements DamageResolver {

  public CactiTracker(TrackerMatchModule tmm, Match match) {
    super(BlockInfo.class, tmm, match);
  }

  @Nullable
  @Override
  public DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    if (damageType == EntityDamageEvent.DamageCause.CONTACT) {
      return blocks().resolveInfo(victim.getLocation().getBlock(), BlockInfo.class);
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlace(ParticipantBlockTransformEvent event) {
    if (event.getNewState().getType() == Material.CACTUS) {
      blocks()
          .trackBlockState(
              event.getNewState(),
              new BlockInfo(event.getNewState().getMaterialData(), event.getPlayerState()));
    }
  }
}
