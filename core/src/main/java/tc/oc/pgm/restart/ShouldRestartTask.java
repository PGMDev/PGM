package tc.oc.pgm.restart;

import java.time.Duration;
import java.time.Instant;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.TimeUtils;

/**
 * Periodically check if the server has exceeded the maximum uptime or memory usage limits and if so
 * flag the server for a restart at the next safest opportunity.
 */
public class ShouldRestartTask implements Runnable {

  private final Instant startTime;

  public ShouldRestartTask() {
    this.startTime = Instant.now();
  }

  @Override
  public void run() {
    if (!RestartManager.isQueued() && hasReachedLimit()) {
      RestartManager.queueRestart("Exceeded uptime limit of " + getLimit());
    }
  }

  private Duration getLimit() {
    return PGM.get().getConfiguration().getUptimeLimit();
  }

  private boolean hasReachedLimit() {
    final Duration limit = getLimit();

    if (TimeUtils.isLongerThan(limit, Duration.ZERO)) {
      final Duration uptime = Duration.between(startTime, Instant.now());
      return TimeUtils.isLongerThan(uptime, limit);
    }

    return false;
  }
}
