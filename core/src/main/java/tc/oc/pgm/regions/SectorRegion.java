package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.RegionDefinition;

public class SectorRegion implements RegionDefinition.HardStatic {
  protected final double x, z;
  protected final double startAngle;
  protected final double endAngle;

  public SectorRegion(double x, double z, double startAngle, double endAngle) {
    this.x = x;
    this.z = z;
    this.startAngle = startAngle;
    this.endAngle = endAngle;
  }

  @Override
  public boolean contains(Vector point) {
    double dx = point.getX() - this.x;
    double dz = point.getZ() - this.z;
    if (dx == 0 && dz == 0) {
      return true;
    }

    double atan2 = Math.atan2(dz, dx);
    if (atan2 < 0) atan2 += 2 * Math.PI;
    return this.startAngle <= atan2 && atan2 <= this.endAngle;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.unbounded();
  }
}
