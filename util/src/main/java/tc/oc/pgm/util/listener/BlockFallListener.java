package tc.oc.pgm.util.listener;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.event.EventUtil.handleCall;

import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import tc.oc.pgm.util.event.block.BlockFallEvent;

/** A listener that calls {@link BlockFallEvent}. */
public class BlockFallListener implements Listener {

  @EventHandler(ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    FallingBlock fallingBlock = MISC_UTILS.getFallingBlock(event);
    if (fallingBlock == null) return;

    handleCall(new BlockFallEvent(event.getBlock(), fallingBlock), event);
  }
}
