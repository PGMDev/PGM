package tc.oc.pgm.restart;

import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.server.ConfigUtils;

/**
 * Periodically check if the server has exceeded the maximum uptime or memory usage limits and if so
 * flag the server for a restart at the next safest opportunity.
 */
public class ShouldRestartTask implements Runnable {

  private final Instant startTime;

  private final Duration uptimeLimit;
  private final long memoryLimit;

  public ShouldRestartTask(Plugin plugin) {
    this.startTime = Instant.now();

    this.uptimeLimit = ConfigUtils.getDuration(plugin.getConfig(), "restart.uptime");
    this.memoryLimit = plugin.getConfig().getLong("restart.memory", 0) * 1024 * 1024; // Megabytes
  }

  @Override
  public void run() {
    if (!RestartManager.isQueued()) {
      if (uptimeLimit()) {
        RestartManager.queueRestart("Exceeded uptime limit of " + uptimeLimit);
      }

      if (memoryLimit()) {
        RestartManager.queueRestart("Exceeded memory limit of " + memoryLimit);
      }
    }
  }

  private boolean uptimeLimit() {
    Duration uptime = new Duration(this.startTime, Instant.now());
    return this.uptimeLimit != null && uptime.isLongerThan(this.uptimeLimit);
  }

  private boolean memoryLimit() {
    long memory = Runtime.getRuntime().totalMemory();
    return this.memoryLimit > 0 && memory > this.memoryLimit;
  }
}
