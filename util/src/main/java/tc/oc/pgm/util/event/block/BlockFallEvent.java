package tc.oc.pgm.util.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public class BlockFallEvent extends Event implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private final Block block;
  private final FallingBlock fallingBlock;
  private boolean cancelled = false;

  public BlockFallEvent(final Block block, final FallingBlock fallingBlock) {
    this.block = block;
    this.fallingBlock = fallingBlock;
  }

  public Block getBlock() {
    return block;
  }

  public FallingBlock getEntity() {
    return fallingBlock;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
