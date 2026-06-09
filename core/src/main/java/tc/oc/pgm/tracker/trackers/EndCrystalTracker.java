package tc.oc.pgm.tracker.trackers;

import org.bukkit.entity.EnderCrystal;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.info.EntityInfo;

@NullMarked
public class EndCrystalTracker extends AbstractTracker<EntityInfo> {
  public EndCrystalTracker(TrackerMatchModule tmm, Match match) {
    super(EntityInfo.class, tmm, match);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCrystalDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

    ParticipantState owner = entities().getOwner(event.getDamager());
    if (owner == null) return;

    entities().trackEntity(crystal, new EntityInfo(crystal, owner));
  }
}
