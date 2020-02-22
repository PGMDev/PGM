package tc.oc.pgm.tracker.trackers;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFallEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.AnvilInfo;

/**
 * Updates the state of owned anvil blocks and entities.
 *
 * <p>TODO: Expand to support all falling blocks
 */
public class AnvilTracker extends AbstractTracker<AnvilInfo> {

  public AnvilTracker(TrackerMatchModule tmm, Match match) {
    super(AnvilInfo.class, tmm, match);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlace(ParticipantBlockTransformEvent event) {
    if (event.getNewState().getMaterial() == Material.ANVIL) {
      blocks().trackBlockState(event.getNewState(), new AnvilInfo(event.getPlayerState()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLand(EntityBlockFormEvent event) {
    AnvilInfo info = resolveEntity(event.getEntity());
    if (info != null) blocks().trackBlockState(event.getNewState(), info);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onFall(BlockFallEvent event) {
    AnvilInfo info = resolveBlock(event.getBlock());
    if (info != null) entities().trackEntity(event.getEntity(), info);
  }
}
