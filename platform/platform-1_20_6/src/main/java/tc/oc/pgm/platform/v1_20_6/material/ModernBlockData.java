package tc.oc.pgm.platform.v1_20_6.material;

import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModernBlockMaterialData that)) return false;
    return Objects.equals(data, that.getBlock());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(data);
  }
}
