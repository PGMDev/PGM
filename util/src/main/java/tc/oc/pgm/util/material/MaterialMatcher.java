package tc.oc.pgm.util.material;

import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/** A predicate on world */
public interface MaterialMatcher {

  boolean matches(Material material);

  boolean matches(MaterialData materialData);

  boolean matches(ItemStack stack);

  /**
   * Iterates over ALL matching {@link Material}s. This can be a long list if the matching criteria
   * is very broad.
   */
  Collection<Material> getMaterials();
}
