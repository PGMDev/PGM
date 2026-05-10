package tc.oc.pgm.entity.kits;

import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.xml.InvalidXMLException;

public record MobHorseKit(
    @Nullable ItemStack saddle,
    @Nullable ItemStack armor,
    @Nullable ItemStack decor) implements MobKit {

  @Override
  public void apply(LivingEntity entity) {
    if (saddle != null) ((Horse) entity).getInventory().setSaddle(saddle);
    if (armor != null) ((Horse) entity).getInventory().setArmor(armor);
    if (decor != null) MobEquipmentUtil.EQUIPMENT.setLlamaDecor(entity, decor);
  }

  @Override
  public void validate(Class<? extends LivingEntity> mobType, Element source)
      throws InvalidXMLException {
    if ((saddle != null || armor != null) && !Horse.class.isAssignableFrom(mobType)) {
      throw new InvalidXMLException(
          "saddle and horse-armor require a horse, got " + mobType.getSimpleName(), source);
    }
    if (decor != null && !MobEquipmentUtil.EQUIPMENT.isLlama(mobType)) {
      throw new InvalidXMLException(
          "llama-decor requires a llama, got " + mobType.getSimpleName(), source);
    }
  }
}
