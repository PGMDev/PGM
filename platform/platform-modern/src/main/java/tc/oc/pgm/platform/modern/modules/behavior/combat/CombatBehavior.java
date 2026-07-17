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
  private final boolean returnHome;
  private final @Nullable Float strayDis;
  private final @Nullable Duration returnAfter;
  private final boolean wander;
  private final boolean panic;
  private final @Nullable Duration panicDuration;

  public CombatBehavior(
      HostilityType hostility,
      @Nullable Region home,
      @Nullable Float radius,
      @Nullable Duration duration,
      float range,
      @Nullable Duration interval,
      boolean returnHome,
      @Nullable Float strayDis,
      @Nullable Duration returnAfter,
      boolean wander,
      boolean panic,
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

  public boolean getReturnHome() {
    return returnHome;
  }

  public @Nullable Float getStrayDis() {
    return strayDis;
  }

  public @Nullable Duration getReturnAfter() {
    return returnAfter;
  }

  public boolean isWander() {
    return wander;
  }

  public boolean isPanic() {
    return panic;
  }

  public @Nullable Duration getPanicDuration() {
    return panicDuration;
  }
}
