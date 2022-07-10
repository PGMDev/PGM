package tc.oc.pgm.util;

import org.bukkit.material.MaterialData;

/** Simple util class to store {@link MaterialData} for a specific location. */
public class MaterialDataWithLocation {
  public final MaterialData data;
  public final int x;
  public final int y;
  public final int z;

  public MaterialDataWithLocation(MaterialData data, int x, int y, int z) {
    this.data = data;
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
