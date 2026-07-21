package tc.oc.pgm.platform.modern.modules.behavior.combat;

import java.time.Duration;
import javax.annotation.Nullable;

public class CombatBehavior {
  private final HostilityType hostility;
  private final @Nullable Duration duration;
  private final float range;
  private final Duration interval;

  public CombatBehavior(
      HostilityType hostility,
      @Nullable Duration duration,
      float range,
      @Nullable Duration interval) {
    this.hostility = hostility;
    this.duration = duration;
    this.range = range;
    this.interval = interval;
  }

  public HostilityType getHostility() {
    return hostility;
  }

  public @Nullable Duration getDuration() {
    return duration;
  }

  public float getRange() {
    return range;
  }

  public Duration getInterval() {
    return interval;
  }
}
