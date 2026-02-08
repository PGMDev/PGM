package tc.oc.pgm.util.event;

import static tc.oc.pgm.util.Assert.assertNotNull;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.block.RayBlockIntersection;

/** An event when a {@link Player} punches a block. */
@Getter
public class PlayerPunchBlockEvent extends PlayerBlockEvent implements Cancellable {

  /** The {@link RayBlockIntersection} of the punch. */
  private final RayBlockIntersection ray;

  public PlayerPunchBlockEvent(
      final Event cause, final Player player, final RayBlockIntersection ray) {
    super(cause, player, assertNotNull(ray).getBlock());
    this.ray = ray;
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
