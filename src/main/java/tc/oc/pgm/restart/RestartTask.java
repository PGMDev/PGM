package tc.oc.pgm.restart;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.event.CancelRestartEvent;
import tc.oc.pgm.api.event.RequestRestartEvent;
import tc.oc.server.ConfigUtils;
import tc.oc.util.logging.ClassLogger;

public class RestartTask implements Runnable {

  private static RestartTask instance;
  private final Plugin plugin;
  private final Logger logger;
  private final Instant startTime; // Time when the server was turned on
  private final Set<RequestRestartEvent.Deferral> deferrals = new HashSet<>();

  private Instant queuedAt;
  private String reason;
  private Duration delayDuration;

  public RestartTask(Plugin plugin) {
    checkState(instance == null);
    instance = this;

    this.plugin = plugin;
    this.logger = ClassLogger.get(this.plugin.getLogger(), this.getClass());
    this.startTime = Instant.now();
  }

  @Override
  public void run() {
    // Runs on an interval to determine if the server should attempt a restart.
    if (isRequested() && this.deferrals.isEmpty()) {
      this.plugin.getServer().getPluginManager().callEvent(new RequestRestartEvent(this));
    } else {
      Duration uptime = new Duration(this.startTime, Instant.now());
      Duration uptimeLimit = ConfigUtils.getDuration(this.plugin.getConfig(), "restart.uptime");

      if (uptimeLimit != null && uptime.isLongerThan(uptimeLimit)) {
        this.queueRestart("Exceeded uptime limit (" + uptime + " > " + uptimeLimit + ")");
      } else {
        long memory = Runtime.getRuntime().totalMemory();
        long memoryLimit =
            this.plugin.getConfig().getLong("restart.memory", 0) * 1024 * 1024; // Megabytes

        if (memoryLimit > 0 && memory > memoryLimit) {
          this.queueRestart("Exceeded memory limit (" + memory + " > " + memoryLimit + ")");
        }
      }
    }
  }

  private boolean queueRestart(String reason) {
    if (!isRequested()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
      this.delayDuration = Config.AutoRestart.time();
      return true;
    }
    return false;
  }

  public boolean queueDelayedRestart(String reason, Duration delay) {
    if (!isRequested()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
      this.delayDuration = delay;
      return true;
    }
    return false;
  }

  public void startRestart(String reason) {
    if (queueRestart(reason)) {
      this.plugin.getServer().getPluginManager().callEvent(new RequestRestartEvent(this));
    }
  }

  public void startDelayedRestart(String reason, Duration delay) {
    if (queueDelayedRestart(reason, delay)) {
      this.plugin.getServer().getPluginManager().callEvent(new RequestRestartEvent(this));
    }
  }

  public void cancelRestart() {
    if (isRequested()) {
      this.queuedAt = null;
      this.reason = null;
      this.delayDuration = null;
      this.plugin.getServer().getPluginManager().callEvent(new CancelRestartEvent());
    }
  }

  public static RestartTask get() {
    return instance;
  }

  public @Nullable Instant getQueuedAt() {
    return queuedAt;
  }

  public @Nullable String getReason() {
    return reason;
  }

  public @Nullable Duration getDelayDuration() {
    return delayDuration;
  }

  public boolean isRequested() {
    return getQueuedAt() != null;
  }

  public boolean isDeferred() {
    return !this.deferrals.isEmpty();
  }

  public boolean isDeferredBy(RequestRestartEvent.Deferral deferral) {
    return this.deferrals.contains(deferral);
  }

  public void addDeferral(RequestRestartEvent.Deferral deferral) {
    if (isRequested()) {
      this.deferrals.add(deferral);
    }
  }

  public void removeDeferral(RequestRestartEvent.Deferral deferral) {
    this.deferrals.remove(deferral);
  }
}
