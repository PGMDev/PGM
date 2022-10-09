package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.MaterialMatcher;

public class CompoundMaterialMatcher implements MaterialMatcher {

  private final Collection<MaterialMatcher> children;
  private @Nullable Collection<Material> materials;

  public CompoundMaterialMatcher(Collection<MaterialMatcher> children) {
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
  public Collection<Material> getMaterials() {
    if (materials == null) {
      Set<Material> materialSet = EnumSet.noneOf(Material.class);
      for (MaterialMatcher child : children) {
        materialSet.addAll(child.getMaterials());
      }
      materials = materialSet;
    }
    return materials;
  }

  public static MaterialMatcher of(Collection<MaterialMatcher> matchers) {
    if (matchers.isEmpty()) {
      return NoMaterialMatcher.INSTANCE;
    } else if (matchers.size() == 1) {
      return matchers.iterator().next();
    } else {
      return new CompoundMaterialMatcher(matchers);
    }
  }
}
