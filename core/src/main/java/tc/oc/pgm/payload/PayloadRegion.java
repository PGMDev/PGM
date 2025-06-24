package tc.oc.pgm.payload;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.function.Supplier;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.regions.Bounds;

/** This is a region that is not immutable. The origin point of the cylinder can move. */
public class PayloadRegion implements RegionDefinition.HardStatic {

  private final Supplier<Vector> base;
  private final double radius;
  private final double radiusSq;

  public PayloadRegion(Supplier<Vector> base, double radius) {
    assertTrue(radius >= 0);

    this.base = base;
    this.radius = radius;
    this.radiusSq = Math.pow(radius, 2);
  }

  @Override
  public boolean contains(Vector point) {
    Vector base = this.base.get();

    return point.getY() >= (base.getY() - 2.5)
        && point.getY() <= (base.getY() + 2.5)
        && Math.pow(point.getX() - base.getX(), 2) + Math.pow(point.getZ() - base.getZ(), 2)
            < this.radiusSq;
  }

  @Override
  public boolean isBlockBounded() {
    return !Double.isInfinite(radius);
  }

  @Override
  public Bounds getBounds() {
    Vector base = this.base.get();
    return new Bounds(
        new Vector(base.getX() - this.radius, base.getY() - 2.5, base.getZ() - this.radius),
        new Vector(base.getX() + this.radius, base.getY() + 2.5, base.getZ() + this.radius));
  }

  @Override
  public String toString() {
    return "PayloadRegion{base=[" + this.base.get() + "],radiusSq=" + this.radiusSq + "}";
  }
}
