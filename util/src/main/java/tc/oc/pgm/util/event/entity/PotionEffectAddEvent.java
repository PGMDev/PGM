package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.event.SportPaper;

/** Called when a potion effect is added to an entity for whatever reason */
@SportPaper
public class PotionEffectAddEvent extends PotionEffectEvent implements Cancellable {
  private boolean cancelled;
  private final EffectAddReason reason;
  private static final HandlerList handlers = new HandlerList();

  public PotionEffectAddEvent(LivingEntity what, PotionEffect effect, EffectAddReason reason) {
    super(what, effect);
    this.reason = reason;
  }

  public boolean isCancelled() {
    return this.cancelled;
  }

  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  public EffectAddReason getReason() {
    return this.reason;
  }

  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public static enum EffectAddReason {
    COMMAND,
    GOLDEN_APPLE,
    BEACON,
    WITHER_SKULL,
    WITHER_SKELETON,
    VILLAGER_CURE,
    VILLAGER_LEVELUP,
    SPIDER_POWERUP,
    POTION_SPLASH,
    POTION_DRINK,
    CUSTOM,
    UNKNOWN;

    private EffectAddReason() {}
  }
}
