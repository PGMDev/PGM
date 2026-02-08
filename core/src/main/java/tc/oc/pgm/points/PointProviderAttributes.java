package tc.oc.pgm.points;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public class PointProviderAttributes {
  private final @Nullable AngleProvider yawProvider;
  private final @Nullable AngleProvider pitchProvider;
  private final boolean safe;
  private final boolean outdoors;

  public PointProviderAttributes(
      @Nullable AngleProvider yawProvider,
      @Nullable AngleProvider pitchProvider,
      boolean safe,
      boolean outdoors) {
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
}
