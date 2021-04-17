package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

/** Matches all world */
public class AllMaterialMatcher implements MaterialMatcher {

  public static final AllMaterialMatcher INSTANCE = new AllMaterialMatcher();

  private AllMaterialMatcher() {}

  @Override
  public boolean matches(Material material) {
    return true;
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return true;
  }

  @Override
  public boolean matches(ItemStack stack) {
    return true;
  }

  @Override
  public Collection<Material> getMaterials() {
    return EnumSet.allOf(Material.class);
  }
}
