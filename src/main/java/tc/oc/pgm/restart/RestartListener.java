package tc.oc.pgm.restart;

import java.util.Iterator;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.util.ClassLogger;

public class RestartListener implements Listener {

  private final Plugin plugin;
  private final MatchManager matchManager;
  private final Logger logger;

  private int matchCount;
  private @Nullable Integer matchLimit;
  private @Nullable RequestRestartEvent.Deferral deferral;

  public RestartListener(Plugin plugin, MatchManager matchManager) {
    this.plugin = plugin;
    this.matchManager = matchManager;
    this.logger = ClassLogger.get(this.plugin.getLogger(), this.getClass());
    this.matchLimit = Config.AutoRestart.matchLimit() > 0 ? Config.AutoRestart.matchLimit() : null;
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
      Iterator<Match> iterator = matchManager.getMatches().iterator();
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
                  : Duration.standardSeconds(30);
          this.logger.info("Starting restart countdown from " + countdownTime);
          ctx.start(new RestartCountdown(match), countdownTime);
        }
      }
    }
  }

  @EventHandler
  public void onCancelRestart(CancelRestartEvent event) {
    Iterator<Match> iterator = matchManager.getMatches().iterator();
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
    if (this.matchLimit != null && ++this.matchCount >= this.matchLimit) {
      RestartManager.queueRestart("Reached match limit of " + this.matchLimit);
    }
  }
}
