package tc.oc.pgm.points;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.util.Vector;

public class DirectedYawProvider implements AngleProvider {
  public DirectedYawProvider(Vector target) {
    this.target = assertNotNull(target, "target");
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
