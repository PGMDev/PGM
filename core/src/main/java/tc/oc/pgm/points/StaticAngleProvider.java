package tc.oc.pgm.points;

import org.bukkit.util.Vector;

public record StaticAngleProvider(float measure) implements AngleProvider {

  @Override
  public float getAngle(Vector from) {
    return this.measure;
  }

  @Override
  public boolean isConstant() {
    return true;
  }
}
