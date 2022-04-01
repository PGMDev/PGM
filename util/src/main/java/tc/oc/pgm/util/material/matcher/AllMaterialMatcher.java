package tc.oc.pgm.util.material.matcher;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
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
  public boolean matches(BlockData blockData) {
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
}
