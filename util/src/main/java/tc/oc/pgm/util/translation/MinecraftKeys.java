package tc.oc.pgm.util.translation;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.StringUtils;

/** Provides access to Minecraft translation keys that are maintained by Mojang. */
public final class MinecraftKeys {
  private MinecraftKeys() {}

  private static final String ENTITY_KEY = "entity.%s.name";
  private static final String MATERIAL_KEY = "item.%s.name";
  private static final String POTION_KEY = "effect.%s";

  private static final Map<Pattern, String> MATERIAL_REPLACEMENTS =
      ImmutableMap.<Pattern, String>builder()
          .put(Pattern.compile("_AXE"), "_HATCHET")
          .put(Pattern.compile("_SPADE"), "_SHOVEL")
          .build();

  private static final Map<String, String> KEY_EXCEPTIONS =
      ImmutableMap.<String, String>builder()
          .put("entity.PrimedTnt.name", "tile.tnt.name")
          .put("entity.MinecartTNT.name", "item.minecartTnt.name")
          .put("entity.EnderCrystal.name", "item.end_crystal.name")
          .put("item.anvil.name", "tile.anvil.name")
          .put("item.sand.name", "tile.sand.name")
          .put("item.gravel.name", "tile.gravel.name")
          .put("item.lava.name", "tile.lava.name")
          .put("effect.speed", "effect.moveSpeed")
          .put("effect.slow", "effect.moveSlowdown")
          .put("effect.fastDigging", "effect.digSpeed")
          .put("effect.slowDigging", "effect.digSlowdown")
          .put("effect.damageResistance", "effect.resistance")
          .build();

  public static String getEntity(EntityType entity) {
    return getKey(ENTITY_KEY, entity.getName());
  }

  // TODO: Bukkit 1.13+ exposes NamespaceKey, which is significantly more accurate than this
  public static String getMaterial(Material material) {
    String name = material.name();
    for (Map.Entry<Pattern, String> entry : MATERIAL_REPLACEMENTS.entrySet()) {
      name = entry.getKey().matcher(name).replaceAll(entry.getValue());
    }

    return getKey(MATERIAL_KEY, StringUtils.camelCase(name, true));
  }

  public static String getPotion(PotionEffectType potion) {
    return getKey(POTION_KEY, StringUtils.camelCase(potion.getName()));
  }

  private static String getKey(String format, String name) {
    final String key = String.format(format, name);
    return KEY_EXCEPTIONS.getOrDefault(key, key);
  }
}
