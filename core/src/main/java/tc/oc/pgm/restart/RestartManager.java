package tc.oc.pgm.restart;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.countdowns.Countdown;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.util.ClassLogger;

public class RestartManager {

  private static final RestartManager INSTANCE = new RestartManager();

  private final PGM plugin;
  private final MatchManager matchManager;
  private final ClassLogger logger;

  private final Set<RequestRestartEvent.Deferral> deferrals = new HashSet<>();

  private Instant queuedAt;
  private String reason;
  private Duration countdown;

  public RestartManager() {
    this.plugin = PGM.get();
    this.matchManager = this.plugin.getMatchManager();
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
  }

  public static RestartManager getInstance() {
    return INSTANCE;
  }

  public void checkRestart() {
    // Check if queued and check for existing deferrals
    if (!isQueued() || isDeferred()) return;

    // Launch the RequestRestartEvent
    RequestRestartEvent event = new RequestRestartEvent();
    Bukkit.getPluginManager().callEvent(event);

    if (isDeferred()) return;

    if (plugin.getServer().getOnlinePlayers().isEmpty()) {
      Bukkit.getServer().shutdown();
      return;
    }

    Iterator<Match> iterator = matchManager.getMatches();
    Match match = iterator.hasNext() ? iterator.next() : null;
    if (match == null) return; // TODO: what now?

    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    ctx.cancelAll();

    //  Start the countdown from the event or create one
    Duration countdownTime =
        getCountdown() != null ? getCountdown() : plugin.getConfiguration().getRestartTime();

    this.logger.info("Starting restart countdown from " + countdownTime);
    Countdown restartCountdown = event.getRestartCountdown() != null
        ? event.getRestartCountdown()
        : new RestartCountdown(match);

    ctx.start(restartCountdown, countdownTime);
  }

  /** Queues a restart to be initiated at next available opportunity. */
  public boolean queueRestart(String reason) {
    return queueRestart(reason, null);
  }

  public boolean queueRestart(String reason, @Nullable Duration countdown) {
    if (!isQueued()) {
      this.queuedAt = Instant.now();
      this.reason = reason;
      this.countdown =
          (countdown != null) ? countdown : plugin.getConfiguration().getRestartTime();

      checkRestart();

      return true;
    }
    return false;
  }

  /** Cancels the restart if there is one already queued */
  public void cancelRestart() {
    if (isQueued()) { // TODO: why this?
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
    return !deferrals.isEmpty();
  }

  public boolean isDeferredBy(RequestRestartEvent.Deferral deferral) {
    return deferrals.contains(deferral);
  }

  public void addDeferral(RequestRestartEvent.Deferral deferral) {
    if (isQueued()) {
      deferrals.add(deferral);
    }
  }

  public void removeDeferral(RequestRestartEvent.Deferral deferral) {
    deferrals.remove(deferral);
    checkRestart();
  }
}
