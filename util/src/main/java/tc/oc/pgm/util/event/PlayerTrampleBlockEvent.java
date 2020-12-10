package tc.oc.pgm.util.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** An event when a {@link Player} tramples over a {@link Block}. */
public class PlayerTrampleBlockEvent extends PlayerBlockEvent {

  public PlayerTrampleBlockEvent(final Event cause, final Player player, final Block block) {
    super(cause, player, block);
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
