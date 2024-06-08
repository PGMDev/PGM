package tc.oc.pgm.util.material.matcher;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class CompoundMaterialMatcher implements MaterialMatcher {

  private final List<MaterialMatcher> children;
  private @Nullable Collection<Material> materials;
  private @Nullable Collection<MaterialData> blockStates;

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
  public boolean matches(org.bukkit.material.MaterialData materialData) {
    for (MaterialMatcher child : children) {
      if (child.matches(materialData)) return true;
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

  public static MaterialMatcher of(Collection<? extends MaterialMatcher> matchers) {
    if (matchers.isEmpty()) {
      return NoMaterialMatcher.INSTANCE;
    } else if (matchers.size() == 1) {
      return matchers.iterator().next();
    } else {
      return new CompoundMaterialMatcher(ImmutableList.copyOf(matchers));
    }
  }

  public static <T> MaterialMatcher of(
      Function<T, MaterialMatcher> mapper, Collection<T> materials) {
    List<MaterialMatcher> matchers = new ArrayList<>(materials.size());
    for (T material : materials) {
      matchers.add(mapper.apply(material));
    }
    return of(matchers);
  }

  @Override
  public String toString() {
    return "CompoundMaterialMatcher{" + "children=" + children + '}';
  }
}
