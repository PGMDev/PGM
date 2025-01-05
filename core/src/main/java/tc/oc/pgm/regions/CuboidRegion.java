package tc.oc.pgm.regions;

import java.util.Random;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class CuboidRegion implements RegionDefinition.HardStatic {
  protected final Bounds bounds;

  public CuboidRegion(Vector pos1, Vector pos2) {
    this(new Bounds(Vector.getMinimum(pos1, pos2), Vector.getMaximum(pos1, pos2)));
  }

  private CuboidRegion(Bounds bounds) {
    this.bounds = bounds;
  }

  @Override
  public boolean contains(Vector point) {
    return this.bounds.contains(point);
  }

  @Override
  public boolean canGetRandom() {
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    return bounds.isFinite();
  }

  @Override
  public Bounds getBounds() {
    return this.bounds;
  }

  @Override
  public Vector getRandom(Random random) {
    if (this.bounds.isEmpty()) {
      throw new ArithmeticException("Region is empty");
    }

    double x = this.randomRange(random, this.bounds.min.getX(), this.bounds.max.getX());
    double y = this.randomRange(random, this.bounds.min.getY(), this.bounds.max.getY());
    double z = this.randomRange(random, this.bounds.min.getZ(), this.bounds.max.getZ());
    return new Vector(x, y, z);
  }

  private double randomRange(Random random, double min, double max) {
    return (max - min) * random.nextDouble() + min;
  }

  public Mutable asMutableCopy() {
    return new Mutable(this.bounds.clone());
  }

  public static class Mutable extends CuboidRegion {
    public Mutable(Bounds bounds) {
      super(bounds);
    }

    public Vector getMutableMin() {
      return bounds.min;
    }

    public Vector getMutableMax() {
      return bounds.max;
    }
  }

  @Override
  public String toString() {
    return "CuboidRegion{min=["
        + this.bounds.min.toString()
        + "],max=["
        + this.bounds.max.toString()
        + "]}";
  }
}
