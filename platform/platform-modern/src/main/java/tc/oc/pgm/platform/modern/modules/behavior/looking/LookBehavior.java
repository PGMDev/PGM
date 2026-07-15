package tc.oc.pgm.platform.modern.modules.behavior.looking;

import org.bukkit.util.Vector;

import javax.annotation.Nullable;



public class LookBehavior {
  private final boolean trackPlayer;
  private final @Nullable Vector trackPoint;
  private final RotationType rotation;
  private final float range;
  private final float restYaw;
  private final float restPitch;

  public LookBehavior(
      boolean trackPlayer,
      @Nullable Vector trackPoint,
      RotationType rotation,
      float range,
      float restYaw,
      float restPitch) {
    this.trackPlayer = trackPlayer;
    this.trackPoint = trackPoint;
    this.rotation = rotation;
    this.range = range;
    this.restYaw = restYaw;
    this.restPitch = restPitch;
  }

  public boolean getTrackPlayer() {
    return trackPlayer;
  }

  public @Nullable Vector getTrackPoint() {
    return trackPoint;
  }

  public RotationType getRotation() {
    return rotation;
  }

  public float getRange() {
    return range;
  }

  public float getRestYaw() {
    return restYaw;
  }

  public float getRestPitch() {
    return restPitch;
  }
}
