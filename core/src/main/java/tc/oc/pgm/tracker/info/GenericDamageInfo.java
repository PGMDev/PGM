package tc.oc.pgm.tracker.info;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.entity.EntityDamageEvent;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.CauseInfo;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.api.tracker.info.PhysicalInfo;

public record GenericDamageInfo(
    EntityDamageEvent.DamageCause damageType, @Nullable PhysicalInfo damager)
    implements DamageInfo, CauseInfo {

  public GenericDamageInfo {
    assertNotNull(damageType);
  }

  public GenericDamageInfo(EntityDamageEvent.DamageCause damageType) {
    this(damageType, null);
  }

  @Override
  public @Nullable PhysicalInfo cause() {
    return damager();
  }

  @Deprecated
  public EntityDamageEvent.DamageCause getDamageType() {
    return damageType();
  }

  @Override
  public @Nullable ParticipantState attacker() {
    return damager == null ? null : damager.owner();
  }
}
