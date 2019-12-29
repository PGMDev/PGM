package tc.oc.pgm.restart;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.joda.time.Duration;
import org.joda.time.Instant;
import tc.oc.pgm.api.event.RequestRestartEvent;

public class RestartManager {

  private static RestartManager instance;
  private final Set<RequestRestartEvent.Deferral> deferrals = new HashSet<>();

  private Instant queuedAt;
  private String reason;
  private Duration countdown;

  public RestartManager() {
    checkState(instance == null);
    instance = this;
  }

  public static RestartManager get() {
    return instance;
  }

  /** Queues a restart to be initiated at next available opportunity. */
  public boolean queueRestart(String reason) {
    if (!isQueued()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
      return true;
    }
    return false;
  }

  public boolean queueRestart(String reason, Duration countdown) {
    if (!isQueued()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
      this.countdown = countdown;
      return true;
    }
    return false;
  }

  /** Cancels the restart if there is one already queued */
  public void cancelRestart() {
    if (isQueued()) {
      this.queuedAt = null;
      this.reason = null;
      this.countdown = null;
    }
  }

  public @Nullable Instant getQueuedAt() {
    return queuedAt;
  }

  public @Nullable String getReason() {
    return reason;
  }

  public @Nullable Duration getCountdown() {
    return countdown;
  }

  public boolean isQueued() {
    return getQueuedAt() != null;
  }

  public boolean isDeferred() {
    return !this.deferrals.isEmpty();
  }

  public boolean isDeferredBy(RequestRestartEvent.Deferral deferral) {
    return this.deferrals.contains(deferral);
  }

  public void addDeferral(RequestRestartEvent.Deferral deferral) {
    if (isQueued()) {
      this.deferrals.add(deferral);
    }
  }

  public void removeDeferral(RequestRestartEvent.Deferral deferral) {
    this.deferrals.remove(deferral);
  }
}
