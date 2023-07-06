package tc.oc.pgm.platform.v1_13.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import tc.oc.pgm.util.nms.material.Rail;

public class Rail1_13 extends MaterialData1_13 implements Rail {
  public Rail1_13(Material material) {
    super(material);
  }

  public Rail1_13(BlockData blockData) {
    super(blockData);
  }

  public Rail1_13(Material material, boolean typeMatters) {
    super(material, typeMatters);
  }

  public Rail1_13(Material material, boolean typeMatters, BlockData blockData) {
    super(material, typeMatters, blockData);
  }

  public Rail1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  @Override
  public boolean dataIsValid() {
    this.blockData = getOrCreateBlockData();
    return blockData instanceof org.bukkit.block.data.Rail;
  }

  @Override
  public byte getDirectionIndex() {
    if (dataIsValid()) {
      return (byte) ((org.bukkit.block.data.Rail) blockData).getShape().ordinal();
    }
    return -1;
  }

  @Override
  public MaterialData1_13 copy() {
    return new Rail1_13(material, blockData, similarMaterials);
  }
}
