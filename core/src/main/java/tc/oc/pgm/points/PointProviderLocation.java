package tc.oc.pgm.points;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The only purpose of this class is to allow callers of {@link PointProvider#getPoint} to detect if
 * the provider set an angle or not, which the CTF module needs to do.
 *
 * <p>The other potential solutions were even more hacky.
 */
public class PointProviderLocation extends Location {

  private boolean hasYaw;
  private boolean hasPitch;

  public PointProviderLocation(World world, double x, double y, double z) {
    super(world, x, y, z);
  }

  public PointProviderLocation(World world, double x, double y, double z, float yaw, float pitch) {
    super(world, x, y, z, yaw, pitch);
    hasYaw = hasPitch = true;
  }

  public boolean hasYaw() {
    return hasYaw;
  }

  public boolean hasPitch() {
    return hasPitch;
  }

  void setHasYaw(boolean yes) {
    hasYaw = yes;
  }

  void setHasPitch(boolean yes) {
    hasPitch = yes;
  }

  @Override
  public PointProviderLocation clone() {
    return (PointProviderLocation) super.clone();
  }
}
