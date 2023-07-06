package tc.oc.pgm.platform.v1_13.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import tc.oc.pgm.util.nms.material.Wool;

public class Wool1_13 extends Colorable1_13 implements Wool {
  public Wool1_13(Material material, boolean typeMatters) {
    super(material, typeMatters);
  }

  public Wool1_13(BlockData blockData) {
    super(blockData);
  }

  public Wool1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  @Override
  public MaterialData1_13 copy() {
    return new Wool1_13(this.material, this.blockData, this.similarMaterials);
  }
}
