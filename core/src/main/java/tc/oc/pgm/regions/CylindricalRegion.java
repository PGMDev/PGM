package tc.oc.pgm.regions;

import static tc.oc.pgm.util.Assert.assertTrue;

import java.util.Random;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class CylindricalRegion implements RegionDefinition.HardStatic {
  private final Vector base;
  private final double radius;
  private final double radiusSq;
  private final double height;

  public CylindricalRegion(Vector base, double radius, double height) {
    assertTrue(radius >= 0);

    this.base = base;
    this.radius = radius;
    this.radiusSq = radius * radius;
    this.height = height;
  }

  @Override
  public boolean contains(Vector point) {
    return point.getY() >= this.base.getY()
        && point.getY() <= (this.base.getY() + this.height)
        && Math.pow(point.getX() - this.base.getX(), 2)
                + Math.pow(point.getZ() - this.base.getZ(), 2)
            < this.radiusSq;
  }

  @Override
  public boolean canGetRandom() {
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    return !Double.isInfinite(radius);
  }

  @Override
  public Bounds getBounds() {
    return new Bounds(
        new Vector(
            this.base.getX() - this.radius, this.base.getY(), this.base.getZ() - this.radius),
        new Vector(
            this.base.getX() + this.radius,
            this.base.getY() + this.height,
            this.base.getZ() + this.radius));
  }

  @Override
  public Vector getRandom(Random random) {
    double angle = random.nextDouble() * Math.PI * 2;
    double hyp = random.nextDouble() + random.nextDouble();
    hyp = (hyp < 1D ? hyp : 2 - hyp) * this.radius;
    double x = Math.cos(angle) * hyp + this.base.getX();
    double z = Math.sin(angle) * hyp + this.base.getZ();
    double y = this.height * random.nextDouble() + this.base.getY();
    return new Vector(x, y, z);
  }

  @Override
  public String toString() {
    return "Cylinder{base=["
        + this.base.toString()
        + "],radius="
        + this.radius
        + ",height="
        + this.height
        + "}";
  }
}
