package tc.oc.pgm.util.material;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.nms.material.MaterialData;

/** A predicate on world */
public interface MaterialMatcher {

  boolean matches(Material material);

  boolean matches(MaterialData materialData);

  boolean matches(ItemStack stack);

  boolean matches(Block block);

  boolean matches(BlockState blockState);

  /**
   * Iterates over ALL matching {@link Material}s. This can be a long list if the matching criteria
   * is very broad.
   */
  Collection<Material> getMaterials();
}
