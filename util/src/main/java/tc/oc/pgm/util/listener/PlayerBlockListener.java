package tc.oc.pgm.util.listener;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;
import tc.oc.pgm.util.nms.NMSHacks;

/** A listener that calls {@link PlayerPunchBlockEvent} and {@link PlayerTrampleBlockEvent}. */
public class PlayerBlockListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerAnimation(final PlayerAnimationEvent event) {
    if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

    // Client cannot punch blocks in adventure mode, so we detect it ourselves.
    RayBlockIntersection hit = NMSHacks.getTargetedBLock(event.getPlayer());
    if (hit == null) return;

    callEvent(new PlayerPunchBlockEvent(event, event.getPlayer(), hit));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCoarseMove(final PlayerCoarseMoveEvent event) {
    if (!event.getPlayer().isOnGround()) return;

    Block block = event.getBlockTo().getBlock();
    if (!block.getType().isSolid()) {
      block = block.getRelative(BlockFace.DOWN);
      if (!block.getType().isSolid()) return;
    }

    callEvent(new PlayerTrampleBlockEvent(event, event.getPlayer(), block));
  }

  private static void callEvent(final Event event) {
    Bukkit.getPluginManager().callEvent(event);
  }
}
