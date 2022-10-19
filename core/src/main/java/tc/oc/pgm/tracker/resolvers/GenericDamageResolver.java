package tc.oc.pgm.tracker.resolvers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.tracker.info.GenericDamageInfo;

public class GenericDamageResolver implements DamageResolver {
  @Override
  public @Nullable DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    if (damager instanceof DamageInfo) {
      // If the damager block/entity resolved to a DamageInfo directly, return that
      return (DamageInfo) damager;
    } else {
      return new GenericDamageInfo(damageType, damager);
    }
  }
}
