package tc.oc.pgm.regions;

import org.bukkit.Chunk;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;

public class Regions {
  private Regions() {}

  public static Region forChunk(Chunk chunk) {
    Vector min = new Vector(chunk.getX() * 16, 0, chunk.getZ() * 16);
    return new CuboidRegion(min, new Vector(min.getX() + 16, 256, min.getZ() + 16));
  }
}
