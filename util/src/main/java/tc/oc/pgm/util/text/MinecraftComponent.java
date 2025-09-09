package tc.oc.pgm.util.text;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.bukkit.EntityTypes;
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
  }
}
