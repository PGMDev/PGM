package tc.oc.pgm.payload;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.function.Supplier;
import org.bukkit.util.Vector;
import tc.oc.pgm.regions.AbstractRegion;
import tc.oc.pgm.regions.Bounds;

/** This is a region that is not immutable. The origin point of the sphere can move. */
public class PayloadRegion extends AbstractRegion {

  private final Supplier<Vector> origin;
  private final double radius;
  private final double radiusSq;

  public PayloadRegion(Supplier<Vector> origin, double radius) {
    checkArgument(radius >= 0);

    this.origin = origin;
    this.radius = radius;
    this.radiusSq = Math.pow(radius, 2);
  }

  @Override
  public boolean contains(Vector point) {
    return origin.get().distanceSquared(point) <= radiusSq;
  }

  @Override
  public boolean isBlockBounded() {
    return !Double.isInfinite(radius);
  }

  @Override
  public Bounds getBounds() {
    Vector diagonal = new Vector(this.radius, this.radius, this.radius);
    return new Bounds(
        origin.get().clone().subtract(diagonal), this.origin.get().clone().add(diagonal));
  }

  @Override
  public String toString() {
    return "PayloadRegion{origin=[" + this.origin.get() + "],radiusSq=" + this.radiusSq + "}";
  }
}
