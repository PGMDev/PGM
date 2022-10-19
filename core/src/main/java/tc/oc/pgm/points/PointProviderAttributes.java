package tc.oc.pgm.points;

import org.jetbrains.annotations.Nullable;

public class PointProviderAttributes {
  private final @Nullable AngleProvider yawProvider;
  private final @Nullable AngleProvider pitchProvider;
  private final boolean safe;
  private final boolean outdoors;

  public PointProviderAttributes(
      AngleProvider yawProvider, AngleProvider pitchProvider, boolean safe, boolean outdoors) {
    this.yawProvider = yawProvider;
    this.pitchProvider = pitchProvider;
    this.safe = safe;
    this.outdoors = outdoors;
  }

  public PointProviderAttributes() {
    this(null, null, false, false);
  }

  public boolean hasValues() {
    return yawProvider != null || pitchProvider != null;
  }

  public @Nullable AngleProvider getYawProvider() {
    return yawProvider;
  }

  public @Nullable AngleProvider getPitchProvider() {
    return pitchProvider;
  }

  public boolean isSafe() {
    return safe;
  }

  public boolean isOutdoors() {
    return outdoors;
  }
}
