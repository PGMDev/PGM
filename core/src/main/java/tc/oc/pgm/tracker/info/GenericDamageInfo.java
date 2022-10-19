package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

public class GenericDamageInfo implements DamageInfo, CauseInfo {

  private final @Nullable PhysicalInfo damager;
  private final EntityDamageEvent.DamageCause damageType;

  public GenericDamageInfo(
      EntityDamageEvent.DamageCause damageType, @Nullable PhysicalInfo damager) {
    this.damageType = assertNotNull(damageType);
    this.damager = damager;
  }

  public GenericDamageInfo(EntityDamageEvent.DamageCause damageType) {
    this(damageType, null);
  }

  public @Nullable PhysicalInfo getDamager() {
    return damager;
  }

  @Override
  public @Nullable PhysicalInfo getCause() {
    return getDamager();
  }

  public EntityDamageEvent.DamageCause getDamageType() {
    return damageType;
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return damager == null ? null : damager.getOwner();
  }
}
