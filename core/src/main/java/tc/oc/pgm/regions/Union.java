package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

public class Union implements RegionDefinition.Static {
  private final Region[] regions;

  public Union(Region... regions) {
    this.regions = regions;
  }

  public static Region of(Region... regions) {
    return switch (regions.length) {
      case 0 -> EmptyRegion.INSTANCE;
      case 1 -> regions[0];
      default -> new Union(regions);
    };
  }

  public Region[] getRegions() {
    return regions;
  }

  @Override
  public boolean contains(Vector point) {
    for (Region region : this.regions) {
      if (region.getStatic().contains(point)) {
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
      if (!region.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    Region[] regions = new Region[this.regions.length];
    for (int i = 0; i < this.regions.length; i++) {
      regions[i] = this.regions[i].getStatic(match);
    }
    return new Union(regions);
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
