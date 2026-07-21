package tc.oc.pgm.platform.modern.modules.behavior.home;

import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.region.Region;

public class HomeBehavior {
  private final Region region;
  private final boolean wander;
  private final @Nullable Float strayDis;
  private final @Nullable Duration returnAfter;

  public HomeBehavior(
      Region region, boolean wander, @Nullable Float strayDis, @Nullable Duration returnAfter) {
    this.region = region;
    this.wander = wander;
    this.strayDis = strayDis;
    this.returnAfter = returnAfter;
  }

  public Region getRegion() {
    return region;
  }

  public boolean isWander() {
    return wander;
  }

  public @Nullable Float getStrayDis() {
    return strayDis;
  }

  public @Nullable Duration getReturnAfter() {
    return returnAfter;
  }
}
