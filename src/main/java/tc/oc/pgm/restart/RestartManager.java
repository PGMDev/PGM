package tc.oc.pgm.restart;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.server.ConfigUtils;
import tc.oc.util.logging.ClassLogger;

/**
 * Manages restarting logic for all plugins. Fires {@link RequestRestartEvent} and {@link
 * CancelRestartEvent} and allows other plugins to defer the restart (see the docs for the event
 * classes).
 *
 * <p>Also monitors uptime and memory use and automatically requests a restart if needed.
 */
public class RestartManager implements Runnable, Listener {
  private static RestartManager instance;

  public static RestartManager get() {
    return instance;
  }

  private final Plugin plugin;
  private final Logger logger;
  private final Instant startTime;
  private final Set<RequestRestartEvent.Deferral> deferrals = new HashSet<>();

  private Instant queuedAt;
  private String reason;

  private int queuedRestartTask;

  public RestartManager(Plugin plugin) {
    checkState(instance == null);
    instance = this;

    this.plugin = plugin;
    this.logger = ClassLogger.get(this.plugin.getLogger(), this.getClass());
    this.startTime = Instant.now();

    this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);

    Duration interval =
        ConfigUtils.getDuration(
            this.plugin.getConfig(), "restart.interval", Duration.standardMinutes(1));
    this.plugin
        .getServer()
        .getScheduler()
        .runTaskTimer(this.plugin, this, 0, interval.getMillis() / 50);
  }

  @Override
  public void run() {
    if (!this.restartIfRequested()) {
      Duration uptime = new Duration(this.startTime, Instant.now());
      Duration uptimeLimit = ConfigUtils.getDuration(this.plugin.getConfig(), "restart.uptime");

      if (uptimeLimit != null && uptime.isLongerThan(uptimeLimit)) {
        this.requestRestart("Exceeded uptime limit (" + uptime + " > " + uptimeLimit + ")");
      } else {
        long memory = Runtime.getRuntime().totalMemory();
        long memoryLimit =
            this.plugin.getConfig().getLong("restart.memory", 0) * 1024 * 1024; // Megabytes

        if (memoryLimit > 0 && memory > memoryLimit) {
          this.requestRestart("Exceeded memory limit (" + memory + " > " + memoryLimit + ")");
        }
      }
    }
  }

  public @Nullable Instant restartRequestedAt() {
    return queuedAt;
  }

  public @Nullable String restartReason() {
    return reason;
  }

  public boolean isRestartRequested() {
    return restartRequestedAt() != null;
  }

  public boolean isRestartDeferred() {
    return !this.deferrals.isEmpty();
  }

  public boolean isRestartDeferredBy(RequestRestartEvent.Deferral deferral) {
    return this.deferrals.contains(deferral);
  }

  public void requestTimedRestart(Duration duration) {
    if (queuedRestartTask > 0) {
      this.plugin.getServer().getScheduler().cancelTask(queuedRestartTask);
      queuedRestartTask = 0;
    }
    Instant queueStamp = Instant.now();
    long ticks = duration.getStandardSeconds() * 20;
    queuedRestartTask =
        Bukkit.getScheduler()
            .runTaskLater(
                this.plugin,
                () -> {
                  queuedRestartTask = 0;
                  if (!this.deferrals.isEmpty()) return;
                  logger.info("Restarting due to request at " + queueStamp);
                  this.plugin.getServer().shutdown();
                },
                ticks)
            .getTaskId();
  }

  public void requestRestart(String reason) {
    if (!this.isRestartRequested()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
    }
  }

  public void cancelRestart() {
    if (queuedRestartTask > 0) {
      this.plugin.getServer().getScheduler().cancelTask(queuedRestartTask);
      queuedRestartTask = 0;
    }
    if (this.isRestartRequested()) {
      this.queuedAt = null;
      this.reason = null;
    }
  }

  public void deferRestart(RequestRestartEvent.Deferral deferral) {
    if (this.isRestartRequested()) {
      this.logger.info("Restart deferred by " + deferral.getPlugin().getName());
      this.deferrals.add(deferral);
    }
  }

  public void resumeRestart(RequestRestartEvent.Deferral deferral) {
    this.logger.info("Restart resumed by " + deferral.getPlugin().getName());
    this.deferrals.remove(deferral);
    this.restartIfRequested();
  }

  public boolean restartIfRequested() {
    if (this.isRestartRequested() && this.deferrals.isEmpty()) {
      this.plugin
          .getServer()
          .getScheduler()
          .runTask(
              this.plugin,
              new Runnable() {
                @Override
                public void run() {
                  logger.info("Restarting due to request at " + restartRequestedAt());
                  plugin.getServer().shutdown();
                }
              });
      return true;
    } else {
      return false;
    }
  }
}
