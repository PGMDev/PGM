package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public abstract class PotionEffectEvent extends Event {

  private final Entity entity;
  private final PotionEffect effect;

  public PotionEffectEvent(Entity entity, PotionEffect effect) {
    this.entity = entity;
    this.effect = effect;
  }

  public LivingEntity getEntity() {
    return (LivingEntity) entity;
  }

  public PotionEffect getEffect() {
    return effect;
  }
}
