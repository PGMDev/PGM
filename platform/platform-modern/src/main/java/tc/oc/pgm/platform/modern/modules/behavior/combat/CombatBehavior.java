package tc.oc.pgm.platform.modern.modules.behavior.combat;

import java.time.Duration;
import javax.annotation.Nullable;
import tc.oc.pgm.api.region.Region;

public class CombatBehavior {
  private final HostilityType hostility;
  private final @Nullable Region home;
  private final @Nullable Float radius;
  private final @Nullable Duration duration;
  private final float range;
  private final Duration interval;
  private final @Nullable Boolean returnHome;
  private final @Nullable Float strayDis;
  private final @Nullable Duration returnAfter;
  private final @Nullable Boolean wander;
  private final @Nullable Boolean panic;
  private final @Nullable Duration panicDuration;

  public CombatBehavior(
      HostilityType hostility,
      @Nullable Region home,
      @Nullable Float radius,
      @Nullable Duration duration,
      float range,
      @Nullable Duration interval,
      @Nullable Boolean returnHome,
      @Nullable Float strayDis,
      @Nullable Duration returnAfter,
      @Nullable Boolean wander,
      @Nullable Boolean panic,
      @Nullable Duration panicDuration) {
    this.hostility = hostility;
    this.home = home;
    this.radius = radius;
    this.duration = duration;
    this.range = range;
    this.interval = interval;
    this.returnHome = returnHome;
    this.strayDis = strayDis;
    this.returnAfter = returnAfter;
    this.wander = wander;
    this.panic = panic;
    this.panicDuration = panicDuration;
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

  public float getRange() {
    return range;
  }

  public Duration getInterval() {
    return interval;
  }

  public @Nullable Boolean getReturnHome() {
    return returnHome;
  }

  public @Nullable Float getStrayDis() {
    return strayDis;
  }

  public Duration getReturnAfter() {
    return returnAfter;
  }

  public @Nullable Boolean isWander() {
    return wander;
  }

  public @Nullable Boolean isPanic() {
    return panic;
  }

  public @Nullable Duration getPanicDuration() {
    return duration;
  }
}
