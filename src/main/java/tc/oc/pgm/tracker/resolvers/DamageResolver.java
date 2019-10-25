package tc.oc.pgm.tracker.resolvers;

import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.tracker.damage.DamageInfo;
import tc.oc.pgm.tracker.damage.PhysicalInfo;

public interface DamageResolver {
  @Nullable
  DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager);
}
