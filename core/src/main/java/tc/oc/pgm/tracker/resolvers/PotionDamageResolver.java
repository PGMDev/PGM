package tc.oc.pgm.tracker.resolvers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.api.tracker.info.PotionInfo;
import tc.oc.pgm.tracker.info.GenericPotionInfo;
import tc.oc.pgm.tracker.info.ProjectileInfo;

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
