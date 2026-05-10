package tc.oc.pgm.platform.sportpaper.entity;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.entity.kits.MobEquipmentUtil;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpMobEquipmentUtil implements MobEquipmentUtil {
  @Override
  public void setOffHand(LivingEntity entity, ItemStack stack) {
    throw new UnsupportedOperationException("Off-hand is not supported on SportPaper 1.8");
  }

  @Override
  public void setOffHandDropChance(LivingEntity entity, float chance) {
    throw new UnsupportedOperationException("Off-hand is not supported on SportPaper 1.8");
  }

  @Override
  public void setLlamaDecor(LivingEntity entity, ItemStack stack) {
    throw new UnsupportedOperationException("Llamas don't exist on SportPaper 1.8");
  }

  @Override
  public boolean isLlama(Class<? extends LivingEntity> type) {
    return false;
  }
}
