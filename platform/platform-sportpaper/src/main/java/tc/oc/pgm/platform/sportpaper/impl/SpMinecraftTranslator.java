package tc.oc.pgm.platform.sportpaper.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.text.MinecraftComponent;

@Supports(value = SPORTPAPER)
public class SpMinecraftTranslator implements MinecraftComponent.MinecraftTranslator {
  private static final String ENTITY_TYPE_FORMAT = "entity.%s.name";

  @Override
  public String getTranslationKey(Material material) {
    var nmsItem = CraftMagicNumbers.getItem(material);
    // This works with most items. Some multi-state items (e.g. dyes or banners) might produce a
    // non-existent translation string, however.
    return nmsItem.getName() + ".name";
  }

  @Override
  @SuppressWarnings("deprecation")
  public String getTranslationKey(EntityType entityType) {
    if (entityType == EntityType.MINECART_TNT)
      return getTranslationKey(Material.EXPLOSIVE_MINECART);
    return ENTITY_TYPE_FORMAT.formatted(entityType.getName());
  }

  @Override
  public String getTranslationKey(PotionEffectType potionEffectType) {
    var potionEffect = (CraftPotionEffectType)
        (potionEffectType instanceof PotionEffectTypeWrapper wrapper
            ? wrapper.getType()
            : potionEffectType);
    return potionEffect.getHandle().a();
  }
}
