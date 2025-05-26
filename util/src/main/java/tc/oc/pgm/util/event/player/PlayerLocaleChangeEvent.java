package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

/** Called when the locale of the player is changed. */
@SportPaper
public class PlayerLocaleChangeEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private final Player player;
  private final String oldLocale;
  private final String newLocale;

  public PlayerLocaleChangeEvent(
      final Player player, final String oldLocale, final String newLocale) {
    this.player = player;
    this.oldLocale = oldLocale;
    this.newLocale = newLocale;
  }

  public Player getPlayer() {
    return player;
  }

  /**
   * Gets the locale the player switched from.
   *
   * @return player's old locale
   */
  public String getOldLocale() {
    return oldLocale;
  }

  /**
   * Gets the locale the player is changed to.
   *
   * @return player's new locale
   */
  public String getNewLocale() {
    return newLocale;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
