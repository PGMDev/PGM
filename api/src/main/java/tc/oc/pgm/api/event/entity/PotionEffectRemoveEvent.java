package tc.oc.pgm.api.event.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.api.event.SportPaper;

/** Called when a potion effect is removed from an entity for whatever reason */
@SportPaper
public class PotionEffectRemoveEvent extends PotionEffectEvent implements Cancellable {

  private boolean cancelled;

  public PotionEffectRemoveEvent(LivingEntity entity, PotionEffect effect) {
    super(entity, effect);
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
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
