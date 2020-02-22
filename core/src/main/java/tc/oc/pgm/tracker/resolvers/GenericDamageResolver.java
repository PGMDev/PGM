package tc.oc.pgm.tracker.resolvers;

import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.tracker.damage.DamageInfo;
import tc.oc.pgm.tracker.damage.GenericDamageInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;

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
