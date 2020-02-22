package tc.oc.pgm.core;

import org.bukkit.block.BlockState;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

public class CoreLeakEvent extends CoreEvent {
  private static final HandlerList handlers = new HandlerList();

  private final BlockState leakedBlock;

  public CoreLeakEvent(Match match, Core core, BlockState leakedBlock) {
    super(match, core);
    this.leakedBlock = leakedBlock;
  }

  public BlockState getLeakedBlock() {
    return this.leakedBlock;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
