package tc.oc.pgm.platform.v1_13.material;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import tc.oc.pgm.util.nms.material.Door;

public class Door1_13 extends MaterialData1_13 implements Door {
  public Door1_13(Material material) {
    super(material);
  }

  public Door1_13(BlockData blockData) {
    super(blockData);
  }

  public Door1_13(Material material, boolean typeMatters) {
    super(material, typeMatters);
  }

  public Door1_13(Material material, boolean typeMatters, BlockData blockData) {
    super(material, typeMatters, blockData);
  }

  public Door1_13(Material material, BlockData blockData, Set<Material> similarMaterials) {
    super(material, blockData, similarMaterials);
  }

  @Override
  public boolean isTopHalf() {
    return ((org.bukkit.block.data.type.Door) getOrCreateBlockData())
        .getHalf()
        .equals(Bisected.Half.TOP);
  }

  @Override
  public MaterialData1_13 copy() {
    return new Door1_13(material, blockData, similarMaterials);
  }
}
