package tc.oc.pgm.util.inventory;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.platform.Platform;

public interface EntityEquipmentUtil {
  EntityEquipmentUtil EQUIPMENT = Platform.get(EntityEquipmentUtil.class);

  void setOffHand(LivingEntity entity, ItemStack stack);

  void setOffHandDropChance(LivingEntity entity, float chance);

  void setLlamaDecor(LivingEntity entity, ItemStack stack);

  boolean isLlama(Class<? extends LivingEntity> type);
}
