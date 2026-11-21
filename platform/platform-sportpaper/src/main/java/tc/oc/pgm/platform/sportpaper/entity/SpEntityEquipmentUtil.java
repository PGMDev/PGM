package tc.oc.pgm.platform.sportpaper.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.EntityEquipmentUtil;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpEntityEquipmentUtil implements EntityEquipmentUtil {
  @Override
  public void setOffHand(LivingEntity entity, ItemStack stack) {
    throw new UnsupportedOperationException("Off-hand is not supported on SportPaper 1.8");
  }

  @Override
  public void setOffHandDropChance(LivingEntity entity, float chance) {
    throw new UnsupportedOperationException("Off-hand is not supported on SportPaper 1.8");
  }

  @Override
  public boolean supportsMobDropChance() {
    return false;
  }

  @Override
  public boolean canEquipSaddle(Class<? extends LivingEntity> type) {
    return Horse.class.isAssignableFrom(type) || Pig.class.isAssignableFrom(type);
  }

  @Override
  public void setSaddle(LivingEntity entity, ItemStack stack) {
    if (entity instanceof Horse horse) horse.getInventory().setSaddle(stack);
    else ((Pig) entity).setSaddle(stack != null);
  }

  @Override
  public void setSaddleDropChance(LivingEntity entity, float chance) {
    throw new UnsupportedOperationException(
        "Saddle drop chance is not supported on SportPaper 1.8");
  }

  @Override
  public boolean canEquipBody(Class<? extends LivingEntity> type) {
    return Horse.class.isAssignableFrom(type);
  }

  @Override
  public void setBody(LivingEntity entity, ItemStack stack) {
    ((Horse) entity).getInventory().setArmor(stack);
  }

  @Override
  public void setBodyDropChance(LivingEntity entity, float chance) {
    throw new UnsupportedOperationException("Body drop chance is not supported on SportPaper 1.8");
  }
}
