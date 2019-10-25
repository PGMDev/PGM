package tc.oc.pgm.points;

import org.bukkit.util.Vector;

public class StaticAngleProvider implements AngleProvider {
  public StaticAngleProvider(float measure) {
    this.measure = measure;
  }

  @Override
  public float getAngle(Vector from) {
    return this.measure;
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  final float measure;
}
