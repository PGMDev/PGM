package tc.oc.pgm.tracker.damage;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.event.entity.EntityDamageEvent;
import tc.oc.pgm.api.player.ParticipantState;

public class GenericDamageInfo implements DamageInfo, CauseInfo {

  private final @Nullable PhysicalInfo damager;
  private final EntityDamageEvent.DamageCause damageType;

  public GenericDamageInfo(
      EntityDamageEvent.DamageCause damageType, @Nullable PhysicalInfo damager) {
    this.damageType = checkNotNull(damageType);
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
