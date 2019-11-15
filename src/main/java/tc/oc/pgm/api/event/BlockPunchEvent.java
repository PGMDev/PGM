package tc.oc.pgm.api.event;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.util.RayBlockIntersection;

/**
 * Called when a {@link Player} punches a {@link org.bukkit.block.Block} in {@link
 * org.bukkit.GameMode#ADVENTURE} mode.
 *
 * @see AdventureModeInteractEvent
 */
public class BlockPunchEvent extends AdventureModeInteractEvent implements Cancellable {

  private final RayBlockIntersection intersection;

  public BlockPunchEvent(Player player, RayBlockIntersection intersection) {
    super(player, checkNotNull(intersection).getBlock());
    this.intersection = intersection;
  }

  /**
   * The {@link RayBlockIntersection} for the {@link AdventureModeInteractEvent}.
   *
   * @return The ray trace to the {@link #getBlock()}.
   */
  public final RayBlockIntersection getIntersection() {
    return intersection;
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
