package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class CircleRegion implements RegionDefinition.HardStatic {
  protected final double x;
  protected final double z;
  protected final double radius;
  protected final double radiusSq;

  public CircleRegion(double x, double z, double radius) {
    this.x = x;
    this.z = z;
    this.radius = radius;
    this.radiusSq = radius * radius;
  }

  @Override
  public boolean contains(Vector point) {
    double dx = point.getX() - this.x;
    double dz = point.getZ() - this.z;
    return Math.pow(dx, 2) + Math.pow(dz, 2) <= this.radiusSq;
  }

  @Override
  public Bounds getBounds() {
    return new Bounds(
        new Vector(this.x - this.radius, Double.NEGATIVE_INFINITY, this.z - this.radius),
        new Vector(this.x + this.radius, Double.POSITIVE_INFINITY, this.z + this.radius));
  }
}
