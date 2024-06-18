package tc.oc.pgm.util.material.matcher;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
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
  public Set<Material> getMaterials() {
    return EnumSet.allOf(Material.class);
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    throw new UnsupportedOperationException("Cannot iterate material data for all materials");
  }

  @Override
  public String toString() {
    return "AllMaterialMatcher{}";
  }
}
