package tc.oc.pgm.util.material.matcher;

import com.google.common.collect.Collections2;
import java.util.Collection;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.nms.material.MaterialData;

public class BlockMaterialMatcher implements MaterialMatcher {

  public static final BlockMaterialMatcher INSTANCE = new BlockMaterialMatcher();

  private BlockMaterialMatcher() {}

  private static final Collection<Material> BLOCKS =
      Collections2.filter(EnumSet.allOf(Material.class), Material::isBlock);

  @Override
  public boolean matches(Material material) {
    return material.isBlock();
  }

  @Override
  public boolean matches(MaterialData materialData) {
    return materialData.isBlock();
  }

  @Override
  public boolean matches(ItemStack stack) {
    return stack.getType().isBlock();
  }

  @Override
  public boolean matches(Block block) {
    return true;
  }

  @Override
  public boolean matches(BlockState blockState) {
    return true;
  }

  @Override
  public Collection<Material> getMaterials() {
    return BLOCKS;
  }
}
