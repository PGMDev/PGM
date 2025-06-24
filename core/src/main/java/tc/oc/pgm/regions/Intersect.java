package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

public class Intersect implements RegionDefinition.Static {
  private final Region[] regions;

  public Intersect(Region... regions) {
    this.regions = regions;
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
    for (Region region : this.regions) {
      if (region.isBlockBounded()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isStatic() {
    for (Region region : this.regions) {
      if (!region.isStatic()) {
        return false;
      }
    }
    return true;
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
  public Region.Static getStaticImpl(Match match) {
    Region[] regions = new Region[this.regions.length];
    for (int i = 0; i < this.regions.length; i++) {
      regions[i] = this.regions[i].getStatic(match);
    }
    return new Intersect(regions);
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
