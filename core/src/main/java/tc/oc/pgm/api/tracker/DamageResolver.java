package tc.oc.pgm.api.tracker;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

public interface DamageResolver {
  @Nullable
  DamageInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager);
}
