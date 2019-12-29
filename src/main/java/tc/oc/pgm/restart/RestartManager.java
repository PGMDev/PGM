package tc.oc.pgm.restart;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.joda.time.Duration;
import org.joda.time.Instant;

public class RestartManager {

  private static final Set<RequestRestartEvent.Deferral> deferrals = new HashSet<>();

  private static Instant queuedAt;
  private static String reason;
  private static Duration countdown;

  /** Queues a restart to be initiated at next available opportunity. */
  public static boolean queueRestart(String reason) {
    if (!isQueued()) {
      RestartManager.queuedAt = Instant.now();
      RestartManager.reason = reason;
      return true;
    }
    return false;
  }

  public static boolean queueRestart(String reason, Duration countdown) {
    if (!isQueued()) {
      RestartManager.queuedAt = Instant.now();
      RestartManager.reason = reason;
      RestartManager.countdown = countdown;
      return true;
    }
    return false;
  }

  /** Cancels the restart if there is one already queued */
  public static void cancelRestart() {
    if (isQueued()) {
      RestartManager.queuedAt = null;
      RestartManager.reason = null;
      RestartManager.countdown = null;
    }
  }

  public static @Nullable Instant getQueuedAt() {
    return queuedAt;
  }

  public static @Nullable String getReason() {
    return reason;
  }

  public static @Nullable Duration getCountdown() {
    return countdown;
  }

  public static boolean isQueued() {
    return getQueuedAt() != null;
  }

  public static boolean isDeferred() {
    return !deferrals.isEmpty();
  }

  public static boolean isDeferredBy(RequestRestartEvent.Deferral deferral) {
    return deferrals.contains(deferral);
  }

  public static void addDeferral(RequestRestartEvent.Deferral deferral) {
    if (isQueued()) {
      deferrals.add(deferral);
    }
  }

  public static void removeDeferral(RequestRestartEvent.Deferral deferral) {
    deferrals.remove(deferral);
  }
}
