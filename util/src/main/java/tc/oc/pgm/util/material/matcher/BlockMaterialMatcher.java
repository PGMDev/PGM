package tc.oc.pgm.util.material.matcher;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class BlockMaterialMatcher implements MaterialMatcher {

  public static final BlockMaterialMatcher INSTANCE = new BlockMaterialMatcher();

  private BlockMaterialMatcher() {}

  private static final Set<Material> BLOCKS =
      Sets.filter(EnumSet.allOf(Material.class), Material::isBlock);

  @Override
  public boolean matches(Material material) {
    return material.isBlock();
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return materialData.getItemType().isBlock();
  }

  @Override
  public boolean matches(ItemStack stack) {
    return stack.getType().isBlock();
  }

  @Override
  public Set<Material> getMaterials() {
    return BLOCKS;
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    throw new UnsupportedOperationException("Cannot iterate material data for all blocks");
  }

  @Override
  public String toString() {
    return "BlockMaterialMatcher{}";
  }
}
