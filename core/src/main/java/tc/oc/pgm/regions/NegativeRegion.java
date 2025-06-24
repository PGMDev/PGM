package tc.oc.pgm.regions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

public class NegativeRegion implements RegionDefinition.Static {
  protected final Region region;

  public NegativeRegion(Region region) {
    this.region = region;
  }

  @Override
  public boolean contains(Vector point) {
    return !this.region.getStatic().contains(point);
  }

  @Override
  public boolean contains(Location point) {
    return !this.region.contains(point);
  }

  @Override
  public boolean isStatic() {
    return region.isStatic();
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return new NegativeRegion(region.getStatic(match));
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
