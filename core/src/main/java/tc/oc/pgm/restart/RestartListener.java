package tc.oc.pgm.restart;

import java.time.Duration;
import java.util.Iterator;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.ClassLogger;

public class RestartListener implements Listener {

  private final PGM plugin;
  private final MatchManager matchManager;
  private final Logger logger;

  private long matchCount;
  private @Nullable RequestRestartEvent.Deferral deferral;

  public RestartListener(PGM plugin, MatchManager matchManager) {
    this.plugin = plugin;
    this.matchManager = matchManager;
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
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

  @EventHandler
  public void onRequestRestart(RequestRestartEvent event) {
    if (this.plugin.getServer().getOnlinePlayers().isEmpty()) {
      Bukkit.getServer().shutdown();
    } else {
      Iterator<Match> iterator = matchManager.getMatches();
      Match match = iterator.hasNext() ? iterator.next() : null;
      if (match != null) {
        this.deferral = event.defer(this.plugin);
        if (match.isRunning()) {
          attemptMatchEnd(match);
        } else {
          SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
          ctx.cancelAll();

          Duration countdownTime =
              RestartManager.getCountdown() != null
                  ? RestartManager.getCountdown()
                  : PGM.get().getConfiguration().getRestartTime();
          this.logger.info("Starting restart countdown from " + countdownTime);
          ctx.start(new RestartCountdown(match), countdownTime);
        }
      }
    }
  }

  @EventHandler
  public void onCancelRestart(CancelRestartEvent event) {
    Iterator<Match> iterator = matchManager.getMatches();
    Match match = iterator.hasNext() ? iterator.next() : null;
    if (match != null) {
      SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
      if (ctx.getCountdown(RestartCountdown.class) != null) {
        this.logger.info("Cancelling restart countdown");
        ctx.cancelAll();
      }
      this.deferral = null;
    }
    RestartManager.cancelRestart();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (RestartManager.isQueued()) {
      this.plugin.getServer().getPluginManager().callEvent(new RequestRestartEvent());
    }
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    attemptMatchEnd(event.getMatch());
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    long matchLimit = plugin.getConfiguration().getMatchLimit();
    if (++matchCount >= matchLimit && matchLimit > 0) {
      RestartManager.queueRestart("Reached match limit of " + matchLimit);
    }
  }
}
