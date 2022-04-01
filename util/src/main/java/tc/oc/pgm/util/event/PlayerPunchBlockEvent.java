package tc.oc.pgm.util.event;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.block.RayBlockIntersection;

/** An event when a {@link Player} punches a block. */
public class PlayerPunchBlockEvent extends PlayerBlockEvent implements Cancellable {

  private final Block targetBlock;

  public PlayerPunchBlockEvent(final Event cause, final Player player, final Block target) {
    super(cause, player, checkNotNull(target));
    this.targetBlock = target;
  }
  /**
   * Gets the {@link RayBlockIntersection} of the punch.
   *
   * @return a ray
   */
  public Block getTargetBlock() {
    return this.targetBlock;
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
