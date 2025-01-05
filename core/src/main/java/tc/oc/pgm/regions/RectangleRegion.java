package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class RectangleRegion implements RegionDefinition.HardStatic {
  protected final double minX;
  protected final double minZ;
  protected final double maxX;
  protected final double maxZ;

  public RectangleRegion(double x1, double z1, double x2, double z2) {
    this.minX = Math.min(x1, x2);
    this.maxX = Math.max(x1, x2);
    this.minZ = Math.min(z1, z2);
    this.maxZ = Math.max(z1, z2);
  }

  @Override
  public boolean contains(Vector point) {
    return (this.minX <= point.getX() && point.getX() <= this.maxX)
        && (this.minZ <= point.getZ() && point.getZ() <= this.maxZ);
  }

  @Override
  public Bounds getBounds() {
    return new Bounds(
        new Vector(this.minX, Double.NEGATIVE_INFINITY, this.minZ),
        new Vector(this.maxX, Double.POSITIVE_INFINITY, this.maxZ));
  }

  @Override
  public String toString() {
    return "Rectangle{min=["
        + this.minX
        + ","
        + this.minZ
        + "],max=["
        + this.maxX
        + ","
        + this.maxZ
        + "]}";
  }
}
