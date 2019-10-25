package tc.oc.pgm.time;

import org.joda.time.Instant;

public class TickTime {
  public final long tick;
  public final Instant instant;

  public TickTime(long tick, Instant instant) {
    this.tick = tick;
    this.instant = instant;
  }

  public long ticksUntil(long then) {
    return Math.max(0, then - tick);
  }

  public long ticksUntil(TickTime then) {
    return ticksUntil(then.tick);
  }

  public long ticksSince(long then) {
    return Math.max(0, tick - then);
  }

  public long ticksSince(TickTime then) {
    return ticksSince(then.tick);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{tick=" + this.tick + " time=" + this.instant + "}";
  }
}
