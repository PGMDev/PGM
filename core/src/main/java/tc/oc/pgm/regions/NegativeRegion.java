package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;

public class NegativeRegion extends AbstractRegion {
  protected final Region region;

  public NegativeRegion(Region region) {
    this.region = region;
  }

  @Override
  public boolean contains(Vector point) {
    return !this.region.contains(point);
  }

  @Override
  public boolean isBlockBounded() {
    return false;
  }

  @Override
  public Bounds getBounds() {
    throw new UnsupportedOperationException("NegativeRegion is unbounded");
  }

  @Override
  public String toString() {
    return "Negative{region=" + this.region.toString() + "}";
  }
}
