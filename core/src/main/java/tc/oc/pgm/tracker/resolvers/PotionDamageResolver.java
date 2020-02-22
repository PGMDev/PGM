package tc.oc.pgm.tracker.resolvers;

import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.tracker.damage.GenericPotionInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.damage.PotionInfo;
import tc.oc.pgm.tracker.damage.ProjectileInfo;

public class PotionDamageResolver implements DamageResolver {

  @Override
  public @Nullable PotionInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    PotionEffectType effect;
    switch (damageType) {
      case POISON:
        effect = PotionEffectType.POISON;
        break;
      case WITHER:
        effect = PotionEffectType.WITHER;
        break;
      case MAGIC:
        effect = null;
        break;
      default:
        return null;
    }

    // If potion is already resolved (i.e. as a splash potion), leave it alone
    if (damager instanceof PotionInfo
        || damager instanceof ProjectileInfo
            && ((ProjectileInfo) damager).getProjectile() instanceof PotionInfo) {
      return null;
    }

    return new GenericPotionInfo(effect);
  }
}
