package tc.oc.pgm.points;

import org.bukkit.util.Vector;

public interface AngleProvider {
  /**
   * Gets an angle for the specified vector.
   *
   * @param from Vector for which the angle should be calculated for.
   * @return Angle measure that may or may not be constant.
   */
  float getAngle(Vector from);

  /**
   * Indicates whether or not the provider has referential transparency.
   *
   * @return Boolean value indicated the constantness of this provider.
   */
  boolean isConstant();
}
