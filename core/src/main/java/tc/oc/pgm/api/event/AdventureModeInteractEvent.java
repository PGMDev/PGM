package tc.oc.pgm.api.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerActionBase;

/**
 * An extended {@link Block} interact event for {@link Player}s sometimes in {@link
 * org.bukkit.GameMode#ADVENTURE} mode.
 */
public abstract class AdventureModeInteractEvent extends PlayerActionBase implements Cancellable {

  private final Block block;
  private boolean cancelled;

  public AdventureModeInteractEvent(Player player, Block block) {
    super(player);
    this.block = block;
  }

  /**
   * Get the {@link Block} that the {@link Player} interacted with.
   *
   * @return The interacted {@link Block}.
   */
  public final Block getBlock() {
    return block;
  }

  @Override
  public final boolean isCancelled() {
    return cancelled;
  }

  @Override
  public final void setCancelled(boolean cancel) {
    cancelled = cancel;
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
