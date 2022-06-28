package tc.oc.pgm.api.blockdrops;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/** The result of breaking a block */
public class BlockDrops {
  public final ImmutableMap<ItemStack, Double> items; // probability -> item
  public final int experience;
  public final @Nullable MaterialData replacement;
  public final @Nullable Float fallChance;
  public final @Nullable Float landChance;
  public final @Nullable Double fallSpeed;

  public BlockDrops(
      Map<ItemStack, Double> items,
      int experience,
      @Nullable MaterialData replacement,
      @Nullable Float fallChance,
      @Nullable Float landChance,
      @Nullable Double fallSpeed) {
    this.items = ImmutableMap.copyOf(items);
    this.experience = experience;
    this.replacement = replacement;
    this.fallChance = fallChance;
    this.landChance = landChance;
    this.fallSpeed = fallSpeed;
  }

  @Override
  public String toString() {
    return this.getClass().getName()
        + " {replacement="
        + this.replacement
        + " items.size="
        + this.items.size()
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
