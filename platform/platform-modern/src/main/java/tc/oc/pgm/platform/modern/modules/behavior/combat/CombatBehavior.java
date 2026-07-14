package tc.oc.pgm.platform.modern.modules.behavior.combat;

import tc.oc.pgm.api.region.Region;

import javax.annotation.Nullable;
import java.time.Duration;

public class CombatBehavior {
  private final HostilityType hostility;
  private final @Nullable Region home;
  private final @Nullable Float radius;
  private final @Nullable Duration duration;
  private final @Nullable Float range;
  private final @Nullable Duration interval;

  public CombatBehavior(
    HostilityType hostility,
    @Nullable Region home,
    @Nullable Float radius,
    @Nullable Duration duration,
    @Nullable Float range,
    @Nullable Duration interval) {
  this.hostility = hostility;
  this.home = home;
  this.radius = radius;
  this.duration = duration;
  this.range = range;
  this.interval = interval;
  }

  public HostilityType getHostility() {
    return hostility;
  }

  public @Nullable Region getHome() {
    return home;
  }

  public @Nullable Float getRadius() {
    return radius;
  }

  public @Nullable Duration getDuration() {
    return duration;
  }

  public @Nullable Float getRange() {
    return range;
  }

  public @Nullable Duration getInterval() {
    return interval;
  }
}
