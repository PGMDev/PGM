package tc.oc.pgm.points;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.util.Vector;

public class DirectedYawProvider implements AngleProvider {
  public DirectedYawProvider(Vector target) {
    this.target = checkNotNull(target, "target");
  }

  @Override
  public float getAngle(Vector from) {
    double dx = this.target.getX() - from.getX();
    double dz = this.target.getZ() - from.getZ();
    return (float) Math.toDegrees(Math.atan2(-dx, dz));
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  Vector target;
}
