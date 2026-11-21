package tc.oc.pgm.util.inventory;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.platform.Platform;

public interface EntityEquipmentUtil {
  EntityEquipmentUtil EQUIPMENT = Platform.get(EntityEquipmentUtil.class);

  void setOffHand(LivingEntity entity, ItemStack stack);

  void setOffHandDropChance(LivingEntity entity, float chance);

  boolean supportsMobDropChance();

  boolean canEquipSaddle(Class<? extends LivingEntity> type);

  void setSaddle(LivingEntity entity, ItemStack stack);

  void setSaddleDropChance(LivingEntity entity, float chance);

  boolean canEquipBody(Class<? extends LivingEntity> type);

  void setBody(LivingEntity entity, ItemStack stack);

  void setBodyDropChance(LivingEntity entity, float chance);
}
