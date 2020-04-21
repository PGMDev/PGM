package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class NoMaterialMatcher implements MaterialMatcher {

  public static final NoMaterialMatcher INSTANCE = new NoMaterialMatcher();

  private NoMaterialMatcher() {}

  @Override
  public boolean matches(Material material) {
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
  public Collection<Material> getMaterials() {
    return Collections.emptySet();
  }
}
