package tc.oc.pgm.util.nms.material.legacy;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.nms.material.Liquid;

public class LiquidLegacy extends MaterialDataLegacy implements Liquid {
  public LiquidLegacy(MaterialData materialData) {
    super(materialData);
  }

  public LiquidLegacy(Material material) {
    super(material);
  }

  @Override
  public void fixDataState(BlockFace blockFace) {
    if (blockFace == BlockFace.DOWN) {
      // A data value of 8 (or higher) represents water flowing down
      this.data = 8;
    } else if (this.data < 7) {
      // Data values 0-7 represent water on the ground, and increase by 1 as they spread
      this.data += 1;
    } else {
      // Otherwise, the previous block must have been flowing down, so it spreads to a data
      // value of 1
      this.data = 1;
    }
  }
}
