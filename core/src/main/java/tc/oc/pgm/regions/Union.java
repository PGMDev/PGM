package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

public class Union implements RegionDefinition {
  private final Region[] regions;

  public Union(Region... regions) {
    this.regions = regions;
  }

  public static Region of(Region... regions) {
    return regions.length == 0
        ? EmptyRegion.INSTANCE
        : regions.length == 1 ? regions[0] : new Union(regions);
  }

  public Region[] getRegions() {
    return regions;
  }

  @Override
  public boolean contains(Vector point) {
    for (Region region : this.regions) {
      if (region.contains(point)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBlockBounded() {
    for (Region region : this.regions) {
      if (!region.isBlockBounded()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Region region : this.regions) {
      if (!region.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Bounds getBounds() {
    Bounds bounds = Bounds.empty();
    for (Region region : this.regions) {
      bounds = Bounds.union(bounds, region.getBounds());
    }
    return bounds;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Union{regions=[");
    for (Region region : this.regions) {
      sb.append(region.toString()).append(",");
    }
    sb.append("]}");
    return sb.toString();
  }
}
