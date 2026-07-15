package tc.oc.pgm.platform.modern.modules.behavior.looking;

import javax.annotation.Nullable;
import org.bukkit.util.Vector;

public class LookBehavior {
  private final boolean trackPlayer;
  private final @Nullable Vector trackPoint;
  private final RotationType rotation;
  private final float range;
  private final @Nullable Float restYaw;
  private final @Nullable Float restPitch;

  public LookBehavior(
      boolean trackPlayer,
      @Nullable Vector trackPoint,
      RotationType rotation,
      float range,
      @Nullable Float restYaw,
      @Nullable Float restPitch) {
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

  public @Nullable Float getRestYaw() {
    return restYaw;
  }

  public @Nullable Float getRestPitch() {
    return restPitch;
  }
}
