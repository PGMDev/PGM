package tc.oc.pgm.platform.modern.registry;

import io.papermc.paper.registry.PaperRegistryBuilder;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * This is the "craft bukkit type" wrapper for vanilla DimensionType. We had to add this ourselves
 * because paper doesn't support it, yet
 *
 * @param key The key
 * @param dimension The vanilla dimension
 */
public record CraftDimensionType(NamespacedKey key, DimensionType dimension) implements Keyed {
  @Override
  public @NotNull NamespacedKey getKey() {
    return key;
  }

  /** Wrapper for dimension type, used in the new registry api */
  public static class Builder implements PaperRegistryBuilder<DimensionType, CraftDimensionType> {
    private DimensionType dimension;

    public Builder(Conversions c, TypedKey<CraftDimensionType> key, DimensionType dimension) {
      this.dimension = dimension;
    }

    public void setDimension(DimensionType dimension) {
      this.dimension = dimension;
    }

    @Override
    public DimensionType build() {
      return dimension;
    }
  }
}
