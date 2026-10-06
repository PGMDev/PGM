package tc.oc.pgm.platform.sportpaper.impl;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import net.kyori.adventure.text.Component;
import net.minecraft.server.v1_8_R3.EnumColor;
import net.minecraft.server.v1_8_R3.LocaleI18n;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
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
    return switch (entityType) {
      case MINECART_TNT -> getTranslationKey(Material.EXPLOSIVE_MINECART);
      case EGG -> getTranslationKey(Material.EGG);
      case ENDER_PEARL -> getTranslationKey(Material.ENDER_PEARL);
      // Not fully correct, but whatever
      case FISHING_HOOK -> getTranslationKey(Material.FISHING_ROD);
      default -> ENTITY_TYPE_FORMAT.formatted(entityType.getName());
    };
  }

  @Override
  public String getTranslationKey(PotionEffectType potionEffectType) {
    var potionEffect = (CraftPotionEffectType)
        (potionEffectType instanceof PotionEffectTypeWrapper wrapper
            ? wrapper.getType()
            : potionEffectType);
    return potionEffect.getHandle().a();
  }

  @Override
  public Component getName(ItemStack item) {
    return translatable(getTranslationKey(item));
  }

  private String getTranslationKey(ItemStack item) {
    // Unlike the material's key, this includes the item's variant, e.g. "item.dyePowder.red"
    var nmsItem = CraftItemStack.asNMSCopy(item);
    if (nmsItem == null) return getTranslationKey(item.getType());

    // Banners are named after their base color, rather than having a variant name
    if (item.getType() == Material.BANNER) {
      var tag = nmsItem.a("BlockEntityTag", false);
      int color = tag != null && tag.hasKey("Base") ? tag.getInt("Base") : nmsItem.getData();
      return "item.banner." + EnumColor.fromInvColorIndex(color).d() + ".name";
    }

    return nmsItem.a() + ".name";
  }

  @Override
  public String getEnglishName(ItemStack item) {
    final String key = getTranslationKey(item);
    final String name = LocaleI18n.get(key);
    // Untranslatable keys are returned as is
    return name.equals(key) ? null : name;
  }
}
