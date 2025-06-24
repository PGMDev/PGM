package tc.oc.pgm.util.material.matcher;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class SingularMaterialMatcher implements MaterialMatcher.Singular {
  private final Material material;

  private SingularMaterialMatcher(Material material) {
    this.material = material;
  }

  @Override
  public boolean matches(Material material) {
    return this.material == material;
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return material == materialData.getItemType();
  }

  @Override
  public boolean matches(ItemStack stack) {
    return material == stack.getType();
  }

  @Override
  public Set<Material> getMaterials() {
    return Set.of(material);
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    return MATERIAL_UTILS.getPossibleBlocks(material);
  }

  public static MaterialMatcher of(Material material) {
    return material == null ? NoMaterialMatcher.INSTANCE : new SingularMaterialMatcher(material);
  }

  @Override
  public String toString() {
    return "SingularMaterialMatcher{" + "material=" + material + '}';
  }
}
