package tc.oc.pgm.blockdrops;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.util.material.BlockMaterialData;

/**
 * The result of breaking a block
 *
 * @param items probability -> item
 */
public record BlockDrops(
    ImmutableMap<ItemStack, Double> items,
    Kit kit,
    int experience,
    @Nullable BlockMaterialData replacement,
    @Nullable Float fallChance,
    @Nullable Float landChance,
    @Nullable Double fallSpeed) {
  public BlockDrops(
      Map<ItemStack, Double> items,
      Kit kit,
      int experience,
      @Nullable BlockMaterialData replacement,
      @Nullable Float fallChance,
      @Nullable Float landChance,
      @Nullable Double fallSpeed) {
    this(
        ImmutableMap.copyOf(items),
        kit,
        experience,
        replacement,
        fallChance,
        landChance,
        fallSpeed);
  }

  @Override
  public @NonNull String toString() {
    return this.getClass().getName()
        + " {replacement="
        + this.replacement
        + " items.size="
        + this.items.size()
        + " kit="
        + this.kit
        + " experience="
        + this.experience
        + " fallChance="
        + this.fallChance
        + " landChance="
        + this.landChance
        + " fallSpeed="
        + this.fallSpeed
        + "}";
  }
}
