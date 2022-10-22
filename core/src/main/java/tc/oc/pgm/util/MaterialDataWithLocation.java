package tc.oc.pgm.util;

import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;

/** Simple util class to store {@link MaterialData} for a specific location. */
public class MaterialDataWithLocation {
  public MaterialData data;
  public BlockVector vector;

  public MaterialDataWithLocation() {}

  public void set(MaterialData data, BlockVector vector) {
    this.data = data;
    this.vector = vector;
  }
}
