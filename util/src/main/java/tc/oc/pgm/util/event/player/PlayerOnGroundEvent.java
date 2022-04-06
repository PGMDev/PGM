package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import tc.oc.pgm.api.event.SportPaper;

@SportPaper
public class PlayerOnGroundEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  private boolean onGround;

  public PlayerOnGroundEvent(final Player player, boolean onGround) {
    super(player);
    this.onGround = onGround;
  }

  /**
   * Returns true of the player is on the ground after the event
   *
   * @return if the player is on the ground
   */
  public boolean getOnGround() {
    return this.onGround;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
