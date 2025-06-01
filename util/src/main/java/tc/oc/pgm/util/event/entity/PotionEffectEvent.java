package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.potion.PotionEffect;
import tc.oc.pgm.util.event.SportPaper;

@SportPaper
public abstract class PotionEffectEvent extends EntityEvent {

  private final PotionEffect effect;

  public PotionEffectEvent(Entity what, PotionEffect effect) {
    super(what);
    this.effect = effect;
  }

  @Override
  public LivingEntity getEntity() {
    return (LivingEntity) super.getEntity();
  }

  public PotionEffect getEffect() {
    return effect;
  }
}
