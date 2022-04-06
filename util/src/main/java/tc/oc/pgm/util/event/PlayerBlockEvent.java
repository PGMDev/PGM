package tc.oc.pgm.util.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.event.GeneralizedEvent;

/** An event when a {@link Player} interacts with a {@link Block}. */
public abstract class PlayerBlockEvent extends GeneralizedEvent {

  private final Player player;
  private final Block block;

  public PlayerBlockEvent(final Event cause, final Player player, final Block block) {
    super(cause);
    this.player = player;
    this.block = block;
  }

  /**
   * Gets the player.
   *
   * @return a player.
   */
  public Player getPlayer() {
    return this.player;
  }

  /**
   * Gets the interacted {@link Block}.
   *
   * @return a block
   */
  public Block getBlock() {
    return this.block;
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
