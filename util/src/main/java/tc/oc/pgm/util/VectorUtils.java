package tc.oc.pgm.util;

import org.bukkit.util.Vector;

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
}
