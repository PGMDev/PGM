package tc.oc.pgm.util.material.matcher;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class CompoundMaterialMatcher implements MaterialMatcher {

  private final List<MaterialMatcher> children;
  private @Nullable Set<Material> materials;

  private CompoundMaterialMatcher(List<MaterialMatcher> children) {
    this.children = children;
  }

  @Override
  public boolean matches(Material material) {
    for (MaterialMatcher child : children) {
      if (child.matches(material)) return true;
    }
    return false;
  }

  @Override
  public boolean matches(MaterialData materialData) {
    for (MaterialMatcher child : children) {
      if (child.matches(materialData)) return true;
    }
    return false;
  }

  @Override
  public boolean matches(ItemStack stack) {
    for (MaterialMatcher child : children) {
      if (child.matches(stack)) return true;
    }
    return false;
  }

  @Override
  public Set<Material> getMaterials() {
    if (materials == null) {
      Set<Material> materialSet = EnumSet.noneOf(Material.class);
      for (MaterialMatcher child : children) {
        materialSet.addAll(child.getMaterials());
      }
      materials = materialSet;
    }
    return materials;
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    Set<BlockMaterialData> result = new HashSet<>(children.size());
    for (MaterialMatcher child : children) result.addAll(child.getPossibleBlocks());
    return result;
  }

  public static MaterialMatcher of(Collection<? extends MaterialMatcher> matchers) {
    return switch (matchers.size()) {
      case 0 -> NoMaterialMatcher.INSTANCE;
      case 1 -> matchers.iterator().next();
      default -> new CompoundMaterialMatcher(ImmutableList.copyOf(matchers));
    };
  }

  @Override
  public String toString() {
    return "CompoundMaterialMatcher{" + "children=" + children + '}';
  }
}
