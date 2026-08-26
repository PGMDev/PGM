package tc.oc.pgm.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class VectorUtils {
  private VectorUtils() {}

  /**
   * Clamps a velocity vector to ensure legacy compatibility
   *
   * @param vec The vector to clamp
   * @return The clamped velocity vector
   */
  public static Vector clampVelocityVector(Vector vec) {
    return new Vector(
        Math.clamp(vec.getX(), -3.9, 3.9),
        Math.clamp(vec.getY(), -3.9, 3.9),
        Math.clamp(vec.getZ(), -3.9, 3.9));
  }

  public static boolean segmentIntersectsSphere(
      Location from, Location to, Vector center, double radius) {
    return distanceSegmentPointSquared(from, to, center) <= radius * radius;
  }

  private static double distanceSegmentPointSquared(Location from, Location to, Vector point) {
    double dx = to.getX() - from.getX();
    double dy = to.getY() - from.getY();
    double dz = to.getZ() - from.getZ();
    double lengthSq = dx * dx + dy * dy + dz * dz;

    if (lengthSq == 0) return point.distanceSquared(from.toVector());

    double t = ((point.getX() - from.getX()) * dx
            + (point.getY() - from.getY()) * dy
            + (point.getZ() - from.getZ()) * dz)
        / lengthSq;
    t = Math.clamp(t, 0d, 1d);

    double x = from.getX() + dx * t;
    double y = from.getY() + dy * t;
    double z = from.getZ() + dz * t;
    double targetDx = point.getX() - x;
    double targetDy = point.getY() - y;
    double targetDz = point.getZ() - z;
    return targetDx * targetDx + targetDy * targetDy + targetDz * targetDz;
  }
}
