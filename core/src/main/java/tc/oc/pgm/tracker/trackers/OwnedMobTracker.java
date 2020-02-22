package tc.oc.pgm.tracker.trackers;

import java.lang.ref.WeakReference;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerSpawnEntityEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.projectile.EntityLaunchEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.MobInfo;

/** Updates the state of owned mobs with info about the owner. */
public class OwnedMobTracker extends AbstractTracker<MobInfo> {

  private WeakReference<Slime> splitter = new WeakReference<>(null);

  public OwnedMobTracker(TrackerMatchModule tmm, Match match) {
    super(MobInfo.class, tmm, match);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMobSpawn(PlayerSpawnEntityEvent event) {
    ParticipantState owner = match.getParticipantState(event.getPlayer());
    if (event.getEntity() instanceof LivingEntity && owner != null) {
      LivingEntity mob = (LivingEntity) event.getEntity();
      entities().trackEntity(event.getEntity(), new MobInfo(mob, owner));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMobDispense(BlockDispenseEntityEvent event) {
    if (event.getEntity() instanceof LivingEntity) {
      ParticipantState owner = blocks().getOwner(event.getBlock());
      if (owner != null) {
        entities()
            .trackEntity(event.getEntity(), new MobInfo((LivingEntity) event.getEntity(), owner));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMobLaunch(EntityLaunchEvent event) {
    if (event.getEntity() instanceof LivingEntity && event.getSource() instanceof Player) {
      ParticipantState shooter = match.getParticipantState((Player) event.getSource());
      if (shooter != null) {
        entities()
            .trackEntity(event.getEntity(), new MobInfo((LivingEntity) event.getEntity(), shooter));
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSlimeSplit(CreatureSpawnEvent event) {
    switch (event.getSpawnReason()) {
      case SLIME_SPLIT:
        Slime parent = splitter.get();
        if (parent != null) {
          MobInfo info = resolveEntity(parent);
          if (info != null) {
            entities().trackEntity(event.getEntity(), info);
          }
        }
        break;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSlimeSplit(SlimeSplitEvent event) {
    if (event.getCount() > 0 && resolveEntity(event.getEntity()) != null) {
      splitter = new WeakReference<>(event.getEntity());
    }
  }
}
