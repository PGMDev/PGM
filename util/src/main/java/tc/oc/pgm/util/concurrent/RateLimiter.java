package tc.oc.pgm.util.concurrent;

import org.bukkit.Bukkit;

public class RateLimiter {
  private final int minDelay, maxDelay;
  private final int timeRatio;
  private final int tpsRatio;

  private long startedAt = 0;
  private long endedAt = 0;
  private long timedOutUntil = 0;

  public RateLimiter(int minDelay, int maxDelay, int timeRatio, int tpsRatio) {
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.timeRatio = timeRatio;
    this.tpsRatio = tpsRatio;
  }

  public void beforeTask() {
    startedAt = System.currentTimeMillis();
  }

  public void afterTask() {
    endedAt = System.currentTimeMillis();
  }

  public void setTimeout(long until) {
    this.timedOutUntil = until;
    afterTask();
  }

  public long getDelay() {
    long now = System.currentTimeMillis();

    long nextUpdate =
        (endedAt - now)
            + ((endedAt - startedAt) * timeRatio)
            + (long) Math.max(0, (20 - Bukkit.getServer().spigot().getTPS()[0]) * tpsRatio)
            + (timedOutUntil > now ? maxDelay : 0);
    return Math.min(Math.max(minDelay, nextUpdate), maxDelay);
  }
}
