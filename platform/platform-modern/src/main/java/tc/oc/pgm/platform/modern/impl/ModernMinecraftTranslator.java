package tc.oc.pgm.platform.modern.impl;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import io.papermc.paper.adventure.PaperAdventure;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;
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

  @Override
  public Component getName(ItemStack item) {
    return PaperAdventure.asAdventure(CraftItemStack.asNMSCopy(item).getItemName());
  }

  @Override
  public @Nullable String getEnglishName(ItemStack item) {
    return PaperAdventure.asPlain(getName(item), Locale.US);
  }
}
