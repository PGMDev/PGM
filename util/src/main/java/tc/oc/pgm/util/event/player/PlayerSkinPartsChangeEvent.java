package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public class PlayerSkinPartsChangeEvent extends Event {

  private final Player player;

  public PlayerSkinPartsChangeEvent(Player player) {
    this.player = player;
  }

  public Player getPlayer() {
    return player;
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
