package tc.oc.pgm.util.material.matcher;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class MultipleMaterialMatcher implements MaterialMatcher {
  private final Set<Material> materials;

  private MultipleMaterialMatcher(Collection<Material> materials) {
    this.materials = EnumSet.copyOf(materials);
  }

  @Override
  public boolean matches(Material material) {
    return materials.contains(material);
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return materials.contains(materialData.getItemType());
  }

  @Override
  public boolean matches(ItemStack stack) {
    return materials.contains(stack.getType());
  }

  @Override
  public Set<Material> getMaterials() {
    return materials;
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    Set<BlockMaterialData> possibleBlocks = new HashSet<>(materials.size());
    for (Material material : materials) {
      possibleBlocks.addAll(MATERIAL_UTILS.getPossibleBlocks(material));
    }
    return possibleBlocks;
  }

  public static MaterialMatcher of(Collection<Material> materials) {
    return switch (materials.size()) {
      case 0 -> NoMaterialMatcher.INSTANCE;
      case 1 -> SingularMaterialMatcher.of(materials.iterator().next());
      default -> new MultipleMaterialMatcher(materials);
    };
  }

  @Override
  public String toString() {
    return "MultipleMaterialMatcher{" + "materials=" + materials + '}';
  }
}
