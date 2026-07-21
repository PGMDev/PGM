package tc.oc.pgm.platform.modern.modules.behavior.evade;

import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.Filter;

public class EvadeBehavior {
  private final boolean panic;
  private final @Nullable Duration panicDuration;
  private final @Nullable Float avoidRange;
  private final @Nullable Filter avoidFilter;

  public EvadeBehavior(
      boolean panic,
      @Nullable Duration panicDuration,
      @Nullable Float avoidRange,
      @Nullable Filter avoidFilter) {
    this.panic = panic;
    this.panicDuration = panicDuration;
    this.avoidRange = avoidRange;
    this.avoidFilter = avoidFilter;
  }

  public boolean isPanic() {
    return panic;
  }

  public @Nullable Duration getPanicDuration() {
    return panicDuration;
  }

  public @Nullable Float getAvoidRange() {
    return avoidRange;
  }

  public @Nullable Filter getAvoidFilter() {
    return avoidFilter;
  }
}
