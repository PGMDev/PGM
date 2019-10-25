package tc.oc.pgm.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerActionBase;

/** Extra events generated for players in adventure mode */
public class AdventureModeInteractEvent extends PlayerActionBase implements Cancellable {

  private final Block block;

  public AdventureModeInteractEvent(Player player, Block block) {
    super(player);
    this.block = block;
  }

  public Block getBlock() {
    return block;
  }

  // Cancellable boilerplate
  private boolean cancelled;

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    cancelled = cancel;
  }

  // Event boilerplate
  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
