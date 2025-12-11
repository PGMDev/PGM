package tc.oc.pgm.platform.modern.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerRiptideEndEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  private final boolean riptiding;

  public PlayerRiptideEndEvent(final Player player, boolean riptiding) {
    super(player);
    this.riptiding = riptiding;
  }

  public boolean getRiptiding() {
    return this.riptiding;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
