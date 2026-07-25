package tc.oc.pgm.platform.modern.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.EntityEquipmentUtil;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernEntityEquipmentUtil implements EntityEquipmentUtil {
  @Override
  public void setOffHand(LivingEntity entity, ItemStack stack) {
    entity.getEquipment().setItemInOffHand(stack);
  }

  @Override
  public void setOffHandDropChance(LivingEntity entity, float chance) {
    entity.getEquipment().setItemInOffHandDropChance(chance);
  }

  @Override
  public boolean supportsMobDropChance() {
    return true;
  }

  @Override
  public boolean canEquipSaddle(Class<? extends LivingEntity> type) {
    return true;
  }

  @Override
  public void setSaddle(LivingEntity entity, ItemStack stack) {
    entity.getEquipment().setItem(EquipmentSlot.SADDLE, stack);
  }

  @Override
  public void setSaddleDropChance(LivingEntity entity, float chance) {
    entity.getEquipment().setDropChance(EquipmentSlot.SADDLE, chance);
  }

  @Override
  public boolean canEquipBody(Class<? extends LivingEntity> type) {
    return true;
  }

  @Override
  public void setBody(LivingEntity entity, ItemStack stack) {
    entity.getEquipment().setItem(EquipmentSlot.BODY, stack);
  }

  @Override
  public void setBodyDropChance(LivingEntity entity, float chance) {
    entity.getEquipment().setDropChance(EquipmentSlot.BODY, chance);
  }
}
