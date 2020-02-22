package tc.oc.pgm.regions;

import org.bukkit.util.Vector;

public class HalfspaceRegion extends AbstractRegion {
  private final Vector normal; // unit normal
  private final double offset; // parameter of the plane equation

  public HalfspaceRegion(Vector origin, Vector normal) {
    this.normal = normal.clone().normalize();
    this.offset = this.normal.dot(origin);
  }

  @Override
  public boolean contains(Vector point) {
    return this.normal.dot(point) >= offset;
  }

  @Override
  public Bounds getBounds() {
    return Bounds.unbounded();
  }
}
