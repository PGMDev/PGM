package tc.oc.pgm.platform.modern.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.NamespacedKey;

/** PGM's own registry keys, and how match world names map onto them. */
public abstract class PgmKeys {
  public static final String NAMESPACE = "pgm";

  /** The datapack-provided dimension type used for pre-1.18 worlds */
  public static final ResourceKey<DimensionType> LEGACY_OVERWORLD =
      ResourceKey.create(Registries.DIMENSION_TYPE, identifier("legacy_overworld"));

  private static Identifier identifier(String path) {
    return Identifier.fromNamespaceAndPath(NAMESPACE, path);
  }

  /** @return the Bukkit key a match world is registered under */
  public static NamespacedKey worldKey(String worldName) {
    return new NamespacedKey(NAMESPACE, worldName);
  }

  /** @return the dimension a match world is loaded into */
  public static ResourceKey<Level> dimensionKey(String worldName) {
    return ResourceKey.create(Registries.DIMENSION, identifier(worldName));
  }
}
