package tc.oc.pgm.platform.modern.modules.behavior.combat;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

public record CombatBehavior(
    HostilityType hostility, @Nullable Duration duration, float range, Duration interval) {

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
}
