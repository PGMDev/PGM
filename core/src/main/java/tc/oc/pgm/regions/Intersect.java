package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;

public class Intersect extends CompositeRegion {

  public Intersect(Region... regions) {
    super(regions);
  }

  @Override
  protected Intersect rebuild(Region[] regions) {
    return new Intersect(regions);
  }

  @Override
  public boolean contains(Vector point) {
    for (Region region : this.regions) {
      if (!region.getStatic().contains(point)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    return any(Region::isBlockBounded);
  }

  @Override
  public boolean isEmpty() {
    return any(Region::isEmpty);
  }

  @Override
  public Bounds getBounds() {
    Bounds bounds = Bounds.unbounded();
    for (Region region : this.regions) {
      bounds = Bounds.intersection(bounds, region.getBounds());
    }
    return bounds;
  }
}
