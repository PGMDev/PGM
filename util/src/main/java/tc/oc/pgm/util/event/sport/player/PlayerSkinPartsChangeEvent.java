package tc.oc.pgm.util.event.sport.player;

import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import tc.oc.pgm.util.skin.Skin;

public class PlayerSkinPartsChangeEvent extends PlayerEvent {

  private final Set<Skin.Part> parts;

  public PlayerSkinPartsChangeEvent(Player who, Set<Skin.Part> parts) {
    super(who);
    this.parts = parts;
  }

  public Set<Skin.Part> getParts() {
    return parts;
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
