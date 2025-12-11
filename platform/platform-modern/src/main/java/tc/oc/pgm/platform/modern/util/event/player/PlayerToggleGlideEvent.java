package tc.oc.pgm.platform.modern.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerToggleGlideEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  private final boolean gliding;

  public PlayerToggleGlideEvent(Player player, boolean gliding) {
    super(player);
    this.gliding = gliding;
  }

  public boolean getGliding() {
    return gliding;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
