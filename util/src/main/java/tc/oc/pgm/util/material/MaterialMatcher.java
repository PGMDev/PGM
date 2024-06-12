package tc.oc.pgm.util.material;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.matcher.CompoundMaterialMatcher;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

/** A predicate on world */
public interface MaterialMatcher {

  boolean matches(Material material);

  boolean matches(org.bukkit.material.MaterialData materialData);

  boolean matches(MaterialData materialData);

  boolean matches(ItemStack stack);

  /**
   * Iterates over ALL matching {@link Material}s. This can be a long list if the matching criteria
   * is very broad.
   */
  Set<Material> getMaterials();

  Set<MaterialData> getMaterialData();

  static MaterialMatcher of(Material... materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::of, Arrays.asList(materials));
  }

  static MaterialMatcher of(Collection<Material> materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::of, materials);
  }

  static MaterialMatcher of(MaterialData... materials) {
    return CompoundMaterialMatcher.of(SingleMaterialMatcher::of, Arrays.asList(materials));
  }
}
