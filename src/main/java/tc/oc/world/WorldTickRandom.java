package tc.oc.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.bukkit.World;

/** Generates random numbers associated with ticks of the given {@link World} */
public class WorldTickRandom {

  private final World world;
  private final Random random;

  private long tick;
  private final Map<Long, Double> doubles = new HashMap<>();

  public WorldTickRandom(World world, Random random) {
    this.world = world;
    this.random = random;
    this.tick = NMSHacks.getMonotonicTime(this.world);
  }

  private void validateCache() {
    long nowTick = NMSHacks.getMonotonicTime(this.world);
    if (nowTick != this.tick) {
      this.tick = nowTick;
      this.doubles.clear();
    }
  }

  /**
   * Return a random number in the range 0 <= n < 1 that is consistent for the duration of the
   * current tick for the given seed.
   */
  public double nextDouble(long seed) {
    this.validateCache();
    Double n = this.doubles.get(seed);
    if (n == null) {
      n = this.random.nextDouble();
      this.doubles.put(seed, n);
    }
    return n;
  }
}
