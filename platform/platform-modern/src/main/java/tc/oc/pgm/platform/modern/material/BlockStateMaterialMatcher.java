package tc.oc.pgm.platform.modern.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.material.MaterialMatcher;

public class BlockStateMaterialMatcher implements MaterialMatcher.Singular {
  private final BlockData data;

  public BlockStateMaterialMatcher(BlockData data) {
    this.data = data;
  }

  @Override
  public Set<Material> getMaterials() {
    return Set.of(data.getMaterial());
  }

  @Override
  public Material getMaterial() {
    return data.getMaterial();
  }

  @Override
  public boolean matches(Material material) {
    return material == data.getMaterial();
  }

  @Override
  public boolean matches(MaterialData materialData) {
    if (materialData instanceof ModernBlockMaterialData bmd) {
      return data.equals(bmd.getBlock());
    }
    // If there is no specific block data to be checked, just go by material
    return matches(materialData.getItemType());
  }

  @Override
  public boolean matches(ItemStack stack) {
    return matches(stack.getType());
  }

  @Override
  public Set<BlockMaterialData> getPossibleBlocks() {
    return Set.of(new ModernBlockData(this.data));
  }

  @Override
  public String toString() {
    return "BlockStateMaterialMatcher{" + "data=" + data + '}';
  }
}
