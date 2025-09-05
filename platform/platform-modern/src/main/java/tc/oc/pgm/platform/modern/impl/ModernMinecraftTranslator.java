package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.text.MinecraftComponent;

@Supports(value = PAPER, minVersion = "1.21.8")
public class ModernMinecraftTranslator implements MinecraftComponent.MinecraftTranslator {
  @Override
  public String getTranslationKey(Material material) {
    return material.translationKey();
  }

  @Override
  public String getTranslationKey(EntityType type) {
    return type.translationKey();
  }

  @Override
  public String getTranslationKey(PotionEffectType potionEffectType) {
    return potionEffectType.translationKey();
  }
}
