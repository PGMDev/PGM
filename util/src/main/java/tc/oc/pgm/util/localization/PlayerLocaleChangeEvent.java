package tc.oc.pgm.util.localization;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerLocaleChangeEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  private final String oldLocale;
  private final String newLocale;

  public PlayerLocaleChangeEvent(Player player, String newLocale, String oldLocale) {
    super(player);
    this.newLocale = newLocale;
    this.oldLocale = oldLocale;
  }

  public String getNewLocale() {
    return newLocale;
  }

  public String getOldLocale() {
    return oldLocale;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
