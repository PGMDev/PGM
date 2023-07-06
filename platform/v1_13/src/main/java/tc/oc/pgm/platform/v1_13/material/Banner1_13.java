package tc.oc.pgm.platform.v1_13.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import tc.oc.pgm.util.nms.material.Banner;

public class Banner1_13 extends MaterialData1_13 implements Banner {
  public Banner1_13(Material material) {
    super(material);
  }

  public Banner1_13(BlockData blockData) {
    super(blockData);
  }

  public Banner1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  @Override
  public void setFacingDirection(BlockFace direction) {
    this.blockData = getOrCreateBlockData();

    if (this.blockData instanceof Rotatable) {
      ((Rotatable) this.blockData).setRotation(direction);
    } else {
      throw new UnsupportedOperationException("Attempted to set rotation on non rotatable Block!");
    }
  }

  @Override
  public BlockFace getFacingDirection() {
    this.blockData = getOrCreateBlockData();

    if (this.blockData instanceof Rotatable) {
      return ((Rotatable) this.blockData).getRotation();
    }
    throw new UnsupportedOperationException("Attempted to get rotation from non rotatable Block!");
  }

  @Override
  public MaterialData1_13 copy() {
    return new Banner1_13(this.material, this.blockData, this.similarMaterials);
  }
}
