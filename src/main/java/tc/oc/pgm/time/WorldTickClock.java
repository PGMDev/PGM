package tc.oc.pgm.time;

import javax.annotation.Nullable;
import org.bukkit.World;
import org.joda.time.Instant;
import tc.oc.world.NMSHacks;

/**
 * Quantizes time to the ticks of the given World. Guaranteed to return the same time over the
 * duration of any tick.
 */
public class WorldTickClock implements TickClock {
  private final World world;
  private @Nullable TickTime now;

  public WorldTickClock(World world) {
    this.world = world;
  }

  @Override
  public TickTime now() {
    long tick = NMSHacks.getMonotonicTime(this.world);
    if (this.now == null || tick != this.now.tick) {
      this.now = new TickTime(tick, Instant.now());
    }
    return this.now;
  }
}
