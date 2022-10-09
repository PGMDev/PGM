package tc.oc.pgm.tracker.info;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.MeleeInfo;

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
