package tc.oc.pgm.util.material.matcher;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.nms.material.MaterialData;

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
  public boolean matches(Block block) {
    return false;
  }

  @Override
  public boolean matches(BlockState blockState) {
    return false;
  }

  @Override
  public Collection<Material> getMaterials() {
    return Collections.emptySet();
  }
}
