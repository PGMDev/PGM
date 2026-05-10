package tc.oc.pgm.platform.modern.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.entity.kits.MobEquipmentUtil;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernMobEquipmentUtil implements MobEquipmentUtil {
  @Override
  public void setOffHand(LivingEntity entity, ItemStack stack) {
    entity.getEquipment().setItemInOffHand(stack);
  }

  @Override
  public void setOffHandDropChance(LivingEntity entity, float chance) {
    entity.getEquipment().setItemInOffHandDropChance(chance);
  }

  @Override
  public void setLlamaDecor(LivingEntity entity, ItemStack stack) {
    ((Llama) entity).getInventory().setDecor(stack);
  }

  @Override
  public boolean isLlama(Class<? extends LivingEntity> type) {
    return Llama.class.isAssignableFrom(type);
  }
}
