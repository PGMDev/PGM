package tc.oc.pgm.restart;

import static com.google.common.base.Preconditions.checkState;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.CancelRestartEvent;
import tc.oc.pgm.api.event.RequestRestartEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.logging.ClassLogger;

/**
 * Listens for {@link RequestRestartEvent} and defers it until after the current match and the
 * restart countdown. Also listens for {@link CancelRestartEvent} and handles it appropriately.
 *
 * <p>Also keeps count of matches and requests a restart from {@link RestartTask} after the
 * configured limit.
 */
public class RestartListener implements Listener {
  private static RestartListener instance;

  public static RestartListener get() {
    return instance;
  }

  private final Plugin plugin;
  private final Logger logger;

  private int matchCount;
  private @Nullable Integer matchLimit;
  private @Nullable RequestRestartEvent.Deferral deferral;

  public RestartListener(Plugin plugin) {
    checkState(instance == null);
    instance = this;

    this.plugin = plugin;
    this.matchLimit = Config.AutoRestart.matchLimit() > 0 ? Config.AutoRestart.matchLimit() : null;
    this.logger = ClassLogger.get(this.plugin.getLogger(), this.getClass());
  }

  private void startCountdown(Match match, Duration duration) {
    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    ctx.cancelAll();
    this.logger.info("Starting restart countdown from " + duration);
    ctx.start(new RestartCountdown(match), duration);
  }

  private void cancelCountdown(Match match) {
    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    if (ctx.getCountdown(RestartCountdown.class) != null) {
      this.logger.info("Cancelling countdown");
      ctx.cancelAll();
    }
  }

  private void attemptMatchEnd(Match match) {
    if (this.deferral == null) return;

    if (match.isRunning()) {
      if (match.getParticipants().isEmpty()) {
        this.logger.info("Ending empty match due to restart request");
        match.finish();
      }
    }
  }

  /**
   * When a restart is requested, let it restart immediately if the server if empty, otherwise defer
   * the restart and ensure that a countdown is running.
   */
  @EventHandler
  public void onRequestRestart(RequestRestartEvent event) {
    if (this.plugin.getServer().getOnlinePlayers().isEmpty()) {
      Bukkit.getServer().shutdown();
    } else {
      Match match = PGM.get().getMatchManager().getMatches().iterator().next();
      if (match != null) {
        this.deferral = event.defer(this.plugin);
        if (match.isRunning()) {
          attemptMatchEnd(match);
        } else {
          this.startCountdown(match, event.getDelayDuration());
        }
      }
    }
  }

  /**
   * When match ends, start a countdown if a restart is already requested, otherwise check for the
   * match limit and request a restart if needed. This listens on LOW priority so that it takes
   * priority over map cycling.
   */
  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (this.deferral != null) {
      this.startCountdown(event.getMatch(), Config.AutoRestart.time());
    } else if (this.matchLimit != null && this.matchCount >= this.matchLimit) {
      RestartTask.get()
          .startRestart("Reached match limit (" + this.matchCount + " >= " + this.matchLimit + ")");
    }
  }

  /** If the match empties out while a restart is queued, finish the match */
  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    attemptMatchEnd(event.getMatch());
  }

  /** When restart is cancelled, cancel any countdown and discard our deferral */
  @EventHandler
  public void onCancelRestart(CancelRestartEvent event) {
    this.cancelCountdown(PGM.get().getMatchManager().getMatches().iterator().next());
    this.deferral = null;
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    this.matchCount++;
  }

  /**
   * Set the match limit restart to the given number of matches from now, or disable the limit if
   * given null;
   *
   * @return The number of matches from now when the server will restart
   */
  public @Nullable Integer increaseMatchLimit(@Nullable Integer incrementAmount) {
    this.matchLimit = incrementAmount == null ? null : this.matchLimit + incrementAmount;
    return this.matchLimit == null ? null : this.matchLimit - this.matchCount;
  }
}
