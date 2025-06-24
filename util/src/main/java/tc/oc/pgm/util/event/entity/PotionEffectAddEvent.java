package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.event.SportPaper;

/** Called when a potion effect is added to an entity for whatever reason */
@SportPaper
public class PotionEffectAddEvent extends PotionEffectEvent implements Cancellable {
  private boolean cancelled;
  private static final HandlerList handlers = new HandlerList();

  public PotionEffectAddEvent(Entity what, PotionEffect effect) {
    super(what, effect);
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
