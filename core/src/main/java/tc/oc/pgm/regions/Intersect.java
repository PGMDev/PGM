package tc.oc.pgm.regions;

import org.bukkit.util.Vector;

public class Intersect extends AbstractRegion {
  private final Region[] regions;

  public Intersect(Region... regions) {
    this.regions = regions;
  }

  @Override
  public boolean contains(Vector point) {
    for (Region region : this.regions) {
      if (!region.contains(point)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    for (Region region : this.regions) {
      if (region.isBlockBounded()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEmpty() {
    for (Region region : this.regions) {
      if (region.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Bounds getBounds() {
    Bounds bounds = Bounds.unbounded();
    for (Region region : this.regions) {
      bounds = Bounds.intersection(bounds, region.getBounds());
    }
    return bounds;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Intersect{regions=[");
    for (Region region : this.regions) {
      sb.append(region.toString()).append(",");
    }
    sb.append("]}");
    return sb.toString();
  }
}
