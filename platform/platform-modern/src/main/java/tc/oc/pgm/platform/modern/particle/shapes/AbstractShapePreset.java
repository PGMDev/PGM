package tc.oc.pgm.platform.modern.particle.shapes;

public abstract class AbstractShapePreset implements ParticleShape {

  protected final float scale;
  protected final float yaw;
  protected final float pitch;

  protected AbstractShapePreset(float scale, float yaw, float pitch) {
    this.scale = scale;
    this.yaw = yaw;
    this.pitch = pitch;
  }
}
