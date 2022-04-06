package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import tc.oc.pgm.api.event.SportPaper;

@SportPaper
public class PlayerSkinPartsChangeEvent extends PlayerEvent {

  public PlayerSkinPartsChangeEvent(Player who) {
    super(who);
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
