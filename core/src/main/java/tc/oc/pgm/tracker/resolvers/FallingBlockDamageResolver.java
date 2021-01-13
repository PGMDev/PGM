package tc.oc.pgm.tracker.resolvers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.tracker.DamageResolver;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;
import tc.oc.pgm.tracker.info.FallingBlockInfo;

public class FallingBlockDamageResolver implements DamageResolver {
  @Override
  public @Nullable FallingBlockInfo resolveDamage(
      EntityDamageEvent.DamageCause damageType, Entity victim, @Nullable PhysicalInfo damager) {
    if (damageType == EntityDamageEvent.DamageCause.FALLING_BLOCK
        && damager instanceof FallingBlockInfo) {
      return (FallingBlockInfo) damager;
    }
    return null;
  }
}
