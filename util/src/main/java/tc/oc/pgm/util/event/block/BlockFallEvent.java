package tc.oc.pgm.util.event.block;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public class BlockFallEvent extends BlockEvent implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private final FallingBlock fallingBlock;

  @Getter
  @Setter
  private boolean cancelled = false;

  public BlockFallEvent(final Block block, final FallingBlock fallingBlock) {
    super(block);
    this.fallingBlock = fallingBlock;
  }

  public FallingBlock getEntity() {
    return fallingBlock;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
