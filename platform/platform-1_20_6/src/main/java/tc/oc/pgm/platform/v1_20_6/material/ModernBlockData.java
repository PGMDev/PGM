package tc.oc.pgm.platform.v1_20_6.material;

import org.bukkit.block.data.BlockData;

public class ModernBlockData implements ModernBlockMaterialData {
  private final BlockData data;

  public ModernBlockData(BlockData data) {
    this.data = data;
  }

  @Override
  public BlockData getBlock() {
    return data;
  }
}
