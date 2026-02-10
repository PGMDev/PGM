package tc.oc.pgm.points;

import org.jspecify.annotations.Nullable;

public record PointProviderAttributes(
    @Nullable AngleProvider yawProvider,
    @Nullable AngleProvider pitchProvider,
    boolean safe,
    boolean outdoors) {

  public PointProviderAttributes() {
    this(null, null, false, false);
  }

  public boolean hasValues() {
    return yawProvider != null || pitchProvider != null;
  }

  @Deprecated
  public @Nullable AngleProvider getYawProvider() {
    return yawProvider();
  }

  @Deprecated
  public @Nullable AngleProvider getPitchProvider() {
    return pitchProvider();
  }

  @Deprecated
  public boolean isSafe() {
    return safe();
  }

  @Deprecated
  public boolean isOutdoors() {
    return outdoors();
  }
}
