package tc.oc.pgm.util.material;

import java.util.Arrays;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

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

  static MaterialMatcher of(Material... materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::new, Arrays.asList(materials));
  }

  static MaterialMatcher of(Collection<Material> materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::new, materials);
  }

  static MaterialMatcher of(MaterialData... materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::new, Arrays.asList(materials));
  }
}
