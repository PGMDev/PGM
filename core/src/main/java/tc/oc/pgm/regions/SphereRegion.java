package tc.oc.pgm.regions;

import static tc.oc.pgm.util.Assert.assertTrue;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class SphereRegion
    implements RegionDefinition.HardStatic, RegionDefinition.MutableSource<SphereRegion.Mutable> {
  protected final Vector origin;
  protected double radius;
  protected double radiusSq;

  public SphereRegion(Vector origin, double radius) {
    assertTrue(radius >= 0);

    this.origin = origin;
    this.radius = radius;
    this.radiusSq = radius * radius;
  }

  public double getRadius() {
    return this.radius;
  }

  public double getRadiusSquared() {
    return this.radiusSq;
  }

  @Override
  public boolean contains(Vector point) {
    return this.origin.distanceSquared(point) <= this.radiusSq;
  }

  @Override
  public boolean isBlockBounded() {
    return !Double.isInfinite(radius);
  }

  @Override
  public Bounds getBounds() {
    Vector diagonal = new Vector(this.radius, this.radius, this.radius);
    return new Bounds(
        this.origin.clone().subtract(diagonal), this.origin.clone().add(diagonal));
  }

  @Override
  public String toString() {
    return "Sphere{origin=[" + this.origin + "],radiusSq=" + this.radiusSq + "}";
  }

  public SphereRegion.Mutable asMutableCopy() {
    return new Mutable(this.origin.clone(), this.radius);
  }

  public static class Mutable extends SphereRegion implements RegionDefinition.Mutable {
    public Mutable(Vector origin, double radius) {
      super(origin, radius);
    }

    public Vector getMutableOrigin() {
      return origin;
    }

    public void setRadius(double radius) {
      this.radius = radius;
      this.radiusSq = radius * radius;
    }
  }
}
