package tc.oc.pgm.api.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Called when a {@link Player} tramples over a {@link Block}.
 *
 * @see AdventureModeInteractEvent
 */
public class BlockTrampleEvent extends AdventureModeInteractEvent {

  public BlockTrampleEvent(Player player, Block block) {
    super(player, block);
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
