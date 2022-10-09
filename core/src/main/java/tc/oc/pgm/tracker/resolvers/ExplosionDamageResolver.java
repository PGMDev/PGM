package tc.oc.pgm.tracker.resolvers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.tracker.info.ExplosionInfo;

public class ExplosionDamageResolver implements DamageResolver {
  @Override
  public @Nullable ExplosionInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    switch (damageType) {
      case ENTITY_EXPLOSION:
      case BLOCK_EXPLOSION:
        // Bukkit fires block explosion events with a null damager in rare situations
        return damager == null ? null : new ExplosionInfo(damager);

      default:
        return null;
    }
  }
}
