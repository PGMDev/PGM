package tc.oc.pgm.util.text;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.StringUtils;

/** A singleton for accessing {@link Component} translations for Minecraft clients. */
public final class MinecraftTranslations {
  private MinecraftTranslations() {}

  private static final String ENTITY_KEY = "entity.%s.name";
  private static final String MATERIAL_KEY = "item.%s.name";
  private static final String POTION_KEY = "potion.%s";

  // TODO: Bukkit 1.13+ exposes NamespaceKey, which is significantly more accurate than this
  private static final Map<Pattern, String> MATERIAL_REPLACEMENTS =
      ImmutableMap.<Pattern, String>builder()
          .put(Pattern.compile("_AXE"), "_HATCHET")
          .put(Pattern.compile("_SPADE"), "_SHOVEL")
          .build();

  private static final Map<String, String> KEY_EXCEPTIONS =
      ImmutableMap.<String, String>builder()
          .put("entity.PrimedTnt.name", "tile.tnt.name") // "TNT" instead of "Block of TNT"
          .put("entity.MinecartTNT.name", "item.minecartTnt.name")
          .put("item.anvil.name", "tile.anvil.name")
          .put("item.sand.name", "tile.sand.name")
          .put("item.gravel.name", "tile.gravel.name")
          .put("item.lava.name", "tile.lava.name")
          .put("potion.speed", "potion.moveSpeed")
          .put("potion.slow", "potion.moveSlowdown")
          .put("potion.fastDigging", "potion.digSpeed")
          .put("potion.slowDigging", "potion.digSlowdown")
          .put("potion.damageResistance", "potion.resistance")
          .build();

  /**
   * Gets a translated entity name.
   *
   * @param entity An entity type.
   * @return An translated entity name.
   */
  public static Component getEntity(EntityType entity) {
    return getKey(ENTITY_KEY, entity.getName());
  }

  /**
   * Gets a translated material name.
   *
   * <p>Note: is highly inaccurate and is only guaranteed to work with weapons.
   *
   * @param material A material.
   * @return A material name.
   */
  public static Component getMaterial(Material material) {
    String name = material.name();
    for (Map.Entry<Pattern, String> entry : MATERIAL_REPLACEMENTS.entrySet()) {
      name = entry.getKey().matcher(name).replaceAll(entry.getValue());
    }

    return getKey(MATERIAL_KEY, StringUtils.camelCase(name, true));
  }

  /**
   * Gets a translated potion name.
   *
   * @param potion A potion type.
   * @return A potion name.
   */
  public static Component getPotion(PotionEffectType potion) {
    return getKey(POTION_KEY, StringUtils.camelCase(potion.getName()));
  }

  private static Component getKey(String format, String name) {
    final String key = String.format(format, name);
    return TranslatableComponent.of(KEY_EXCEPTIONS.getOrDefault(key, key));
  }
}
