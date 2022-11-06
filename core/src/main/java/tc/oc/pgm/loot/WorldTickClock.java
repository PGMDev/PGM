package tc.oc.pgm.loot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.bukkit.World;
import tc.oc.pgm.util.collection.InstantMap;
import tc.oc.pgm.util.nms.NMSHacks;

/**
 * Quantizes time to the ticks of the given World. Guaranteed to return the same time over the
 * duration of any tick.
 *
 * <p>Does not support anything that requires a {@link ZoneId}, should only be used to fetch
 * instants. Only implements {@link Clock} to fit into {@link InstantMap}
 */
public class WorldTickClock extends Clock {

  private final World world;
  private Instant now;
  private long tick;

  public WorldTickClock(World world) {
    this.world = world;
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
    return this.now();
  }

  private Instant now() {
    long tick = NMSHacks.getMonotonicTime(this.world);
    if (this.now == null || tick != this.tick) {
      this.tick = tick;
      this.now = Instant.now();
    }
    return this.now;
  }
}
