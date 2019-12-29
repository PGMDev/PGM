package tc.oc.pgm.restart;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.joda.time.Duration;
import org.joda.time.Instant;

public class RestartManager {

  private static final Set<RequestRestartEvent.Deferral> _deferrals = new HashSet<>();

  private static Instant _queuedAt;
  private static String _reason;
  private static Duration _countdown;

  /** Queues a restart to be initiated at next available opportunity. */
  public static boolean queueRestart(String reason) {
    if (!isQueued()) {
      _queuedAt = Instant.now();
      _reason = reason;
      return true;
    }
    return false;
  }

  public static boolean queueRestart(String reason, Duration countdown) {
    if (!isQueued()) {
      _queuedAt = Instant.now();
      _reason = reason;
      _countdown = countdown;
      return true;
    }
    return false;
  }

  /** Cancels the restart if there is one already queued */
  public static void cancelRestart() {
    if (isQueued()) {
      _queuedAt = null;
      _reason = null;
      _countdown = null;
    }
  }

  public static @Nullable Instant getQueuedAt() {
    return _queuedAt;
  }

  public static @Nullable String getReason() {
    return _reason;
  }

  public static @Nullable Duration getCountdown() {
    return _countdown;
  }

  public static boolean isQueued() {
    return getQueuedAt() != null;
  }

  public static boolean isDeferred() {
    return !_deferrals.isEmpty();
  }

  public static boolean isDeferredBy(RequestRestartEvent.Deferral deferral) {
    return _deferrals.contains(deferral);
  }

  public static void addDeferral(RequestRestartEvent.Deferral deferral) {
    if (isQueued()) {
      _deferrals.add(deferral);
    }
  }

  public static void removeDeferral(RequestRestartEvent.Deferral deferral) {
    _deferrals.remove(deferral);
  }
}
