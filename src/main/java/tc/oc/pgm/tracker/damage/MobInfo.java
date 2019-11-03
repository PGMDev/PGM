package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import tc.oc.pgm.api.player.ParticipantState;

public class MobInfo extends EntityInfo implements MeleeInfo {

  private final ItemInfo weapon;

  public MobInfo(LivingEntity mob, @Nullable ParticipantState owner) {
    super(mob, owner);
    this.weapon = new ItemInfo(mob.getEquipment().getItemInHand());
  }

  public MobInfo(LivingEntity mob) {
    this(mob, null);
  }

  @Override
  public @Nullable ParticipantState getAttacker() {
    return getOwner();
  }

  @Override
  public ItemInfo getWeapon() {
    return weapon;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{entity="
        + getEntityType()
        + " name="
        + getCustomName()
        + " owner="
        + getOwner()
        + " weapon="
        + getWeapon()
        + "}";
  }
}
