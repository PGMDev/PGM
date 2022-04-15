package tc.oc.pgm.util.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

/** A predicate on world */
public interface MaterialMatcher {

  boolean matches(Material material);

  boolean matches(BlockData blockData);

  boolean matches(ItemStack stack);

  /**
   * Iterates over ALL matching {@link Material}s. This can be a long list if the matching criteria
   * is very broad.
   */
  Set<Material> getMaterials();
}
