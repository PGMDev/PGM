package tc.oc.pgm.regions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;

public class MirroredRegion extends TransformedRegion {
  // Reflection plane equation is: v · normal = offset
  private final Vector normal; // unit normal
  private final double offset; // parameter of the plane equation

  /**
   * @param region The region that will be mirrored
   * @param origin A point on the reflection plane
   * @param normal The normal of the reflection plane
   */
  public MirroredRegion(Region region, Vector origin, Vector normal) {
    super(region);
    this.normal = normal.clone().normalize();
    this.offset = this.normal.dot(origin);
  }

  private MirroredRegion(Region region, Vector normal, double offset) {
    super(region);
    this.normal = normal;
    this.offset = offset;
  }

  @Override
  protected Vector transform(Vector point) {
    // FYI, reflection is 2x the projection of the point on the normal
    Vector reflection = this.normal.clone().multiply(2 * (point.dot(this.normal) - this.offset));
    return point.clone().subtract(reflection);
  }

  @Override
  protected Vector untransform(Vector point) {
    return this.transform(point);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    return new MirroredRegion(region.getStatic(match), normal, offset);
  }
}
