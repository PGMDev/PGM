package tc.oc.pgm.util.material.matcher;

import java.util.Collections;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class NoMaterialMatcher implements MaterialMatcher {

  public static final NoMaterialMatcher INSTANCE = new NoMaterialMatcher();

  private NoMaterialMatcher() {}

  @Override
  public boolean matches(Material material) {
    return false;
  }

  @Override
  public boolean matches(org.bukkit.material.MaterialData materialData) {
    return false;
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return false;
  }

  @Override
  public boolean matches(ItemStack stack) {
    return false;
  }

  @Override
  public Set<Material> getMaterials() {
    return Collections.emptySet();
  }

  @Override
  public Set<MaterialData> getMaterialData() {
    return Collections.emptySet();
  }

  @Override
  public String toString() {
    return "NoMaterialMatcher{}";
  }
}
