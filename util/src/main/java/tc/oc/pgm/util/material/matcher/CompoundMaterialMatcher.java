package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.ImmutableMaterialSet;
import tc.oc.pgm.util.material.MaterialMatcher;

public class CompoundMaterialMatcher implements MaterialMatcher {

  private final Collection<MaterialMatcher> children;
  private @Nullable ImmutableMaterialSet materials;

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
      ImmutableMaterialSet.Builder builder = ImmutableMaterialSet.builder();
      for (MaterialMatcher child : children) {
        for (Material material : child.getMaterials()) {
          builder.add(material);
        }
      }
      materials = builder.build();
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
