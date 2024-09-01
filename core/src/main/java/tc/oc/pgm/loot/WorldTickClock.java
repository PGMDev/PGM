package tc.oc.pgm.loot;

import static tc.oc.pgm.util.nms.NMSHacks.NMS_HACKS;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.collection.InstantMap;

/**
 * Quantizes time to the ticks of the given World. Guaranteed to return the same time over the
 * duration of any tick.
 *
 * <p>Does not support anything that requires a {@link ZoneId}, should only be used to fetch
 * instants. Only implements {@link Clock} to fit into {@link InstantMap}
 */
public class WorldTickClock extends Clock {

  private final Match match;
  private Tick tick;

  public WorldTickClock(Match match) {
    this.match = match;
  }

  @Override
  public ZoneId getZone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Instant instant() {
    return this.now().instant;
  }

  public Tick getTick() {
    return this.now();
  }

  private Tick now() {
    long tick = NMS_HACKS.getMonotonicTime(match.getWorld());
    if (this.tick == null || tick != this.tick.tick) {
      this.tick = new Tick(tick, Instant.now());
    }
    return this.tick;
  }
}
