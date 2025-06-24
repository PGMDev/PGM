package tc.oc.pgm.tracker.trackers;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.EntityInfo;
import tc.oc.pgm.tracker.info.TNTInfo;
import tc.oc.pgm.util.event.block.BlockDispenseEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.event.player.PlayerSpawnEntityEvent;

/** Updates the state of owned TNT blocks and entities */
public class TNTTracker extends AbstractTracker<TNTInfo> {
  public TNTTracker(TrackerMatchModule tmm, Match match) {
    super(TNTInfo.class, tmm, match);
  }

  @SuppressWarnings("deprecation")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlace(ParticipantBlockTransformEvent event) {
    if (event.getNewState().getType() == Material.TNT) {
      blocks()
          .trackBlockState(
              event.getNewState(),
              new TNTInfo(event.getPlayerState(), event.getNewState().getLocation()));
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPrime(ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed) {
      // Some TNT was activated, try to figure out why
      TNTPrimed tnt = (TNTPrimed) event.getEntity();
      TNTInfo info = null;

      if (event instanceof ExplosionPrimeByEntityEvent) {
        Entity primer = ((ExplosionPrimeByEntityEvent) event).getPrimer();
        if (primer instanceof TNTPrimed) {
          // Primed by another owned explosive, propagate the damage info (e.g. origin location)
          info = resolveEntity(primer);
        } else {
          ParticipantState owner = entities().getOwner(primer);
          if (owner != null) {
            // Primed by some other type of owned entity e.g. flaming arrow, pet creeper, etc.
            info = new TNTInfo(owner, tnt.getLocation());
          }
        }
      }

      if (info == null) {
        ParticipantState placer = blocks().getOwner(tnt.getLocation().getBlock());
        if (placer != null) {
          // If no primer was resolved for the event, give the TNT entity to the block placer, if
          // any
          info = new TNTInfo(placer, tnt.getLocation());
        }
      }

      if (info != null) {
        entities().trackEntity(tnt, info);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDispense(BlockDispenseEntityEvent event) {
    if ((event.getEntity() instanceof TNTPrimed)
        || (event.getEntity() instanceof ExplosiveMinecart)) {
      ParticipantState owner = blocks().getOwner(event.getBlock());
      if (owner != null) {
        entities()
            .trackEntity(
                event.getEntity(),
                (event.getEntity() instanceof TNTPrimed)
                    ? new TNTInfo(owner, event.getEntity().getLocation())
                    : new EntityInfo(event.getEntity(), owner));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSpawn(PlayerSpawnEntityEvent event) {
    ParticipantState owner = match.getParticipantState(event.getPlayer());
    if (event.getEntity() instanceof ExplosiveMinecart && owner != null) {
      entities().trackEntity(event.getEntity(), new EntityInfo(event.getEntity(), owner));
    }
  }
}
