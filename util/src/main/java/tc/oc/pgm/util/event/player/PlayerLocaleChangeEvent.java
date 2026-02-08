package tc.oc.pgm.util.event.player;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import tc.oc.pgm.util.event.SportPaper;

/** Called when the locale of the player is changed. */
@Getter
@SportPaper
public class PlayerLocaleChangeEvent extends PlayerEvent {
  private static final HandlerList handlers = new HandlerList();
  /** The locale the player switched from. */
  private final String oldLocale;
  /** The locale the player is changed to. */
  private final String newLocale;

  public PlayerLocaleChangeEvent(
      final Player player, final String oldLocale, final String newLocale) {
    super(player);
    this.oldLocale = oldLocale;
    this.newLocale = newLocale;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
