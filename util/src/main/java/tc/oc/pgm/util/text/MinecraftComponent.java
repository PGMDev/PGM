package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.Component.virtual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.VirtualComponentRenderer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.bukkit.EntityTypes;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.platform.Platform;

/** A singleton for accessing {@link Component} translations for Minecraft clients. */
public final class MinecraftComponent {
  private static final MinecraftTranslator TRANSLATOR = Platform.get(MinecraftTranslator.class);

  private MinecraftComponent() {}

  /**
   * Gets a translated entity name.
   *
   * @param entity An entity type.
   * @return A translated entity name.
   */
  public static Component entity(EntityType entity) {
    if (entity == EntityTypes.PRIMED_TNT)
      // "TNT" instead of "Block of TNT" (1.8) / "Primed TNT" (1.21)
      return translatable(TRANSLATOR.getTranslationKey(Material.TNT));
    else return translatable(TRANSLATOR.getTranslationKey(entity));
  }

  /**
   * Gets a translated material name.
   *
   * <p>Note: won't work with some materials on 1.8 servers.
   *
   * @param material A material.
   * @return A material name.
   */
  public static Component material(Material material) {
    return translatable(TRANSLATOR.getTranslationKey(material));
  }

  /**
   * Gets a translated item name, including its variant, such as the color of a dye.
   *
   * @param item An item.
   * @return An item name.
   */
  public static Component item(ItemStack item) {
    final Component name = TRANSLATOR.getName(item);
    final String english = TRANSLATOR.getEnglishName(item);
    return english == null ? name : virtual(CommandSender.class, new LegacyFallback(name, english));
  }

  /**
   * Shows 1.7 clients a plain English name, since their language files don't have the keys of items
   * added or renamed in 1.8, such as banners or stone variants.
   */
  private record LegacyFallback(Component name, String english)
      implements VirtualComponentRenderer<CommandSender> {
    @Override
    public ComponentLike apply(CommandSender viewer) {
      return viewer instanceof Player player
              && ViaUtils.getProtocolVersion(player) <= ViaUtils.VERSION_1_7
          ? text(english)
          : name;
    }

    @Override
    public String fallbackString() {
      return english;
    }
  }

  /**
   * Gets a translated potion name.
   *
   * @param potion A potion type.
   * @return A potion name.
   */
  public static Component potion(PotionEffectType potion) {
    return translatable(TRANSLATOR.getTranslationKey(potion));
  }

  public interface MinecraftTranslator {
    String getTranslationKey(Material material);

    String getTranslationKey(EntityType entityType);

    String getTranslationKey(PotionEffectType potionEffectType);

    Component getName(ItemStack item);

    @Nullable
    String getEnglishName(ItemStack item);
  }
}
