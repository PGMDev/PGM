package tc.oc.pgm.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.ConfigLoadEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.util.logging.ClassLogger;

/**
 * Listens for {@link RequestRestartEvent} and defers it until after the current match and the
 * restart countdown. Also listens for {@link CancelRestartEvent} and handles it appropriately.
 *
 * <p>Also keeps count of matches and requests a restart from {@link RestartManager} after the
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
  private Duration defaultCountdownTime;
  private @Nullable RequestRestartEvent.Deferral deferral;

  public RestartListener(Plugin plugin) {
    checkState(instance == null);
    instance = this;

    this.plugin = plugin;
    this.logger = ClassLogger.get(this.plugin.getLogger(), this.getClass());

    this.loadConfig(this.plugin.getConfig());
  }

  public void loadConfig(Configuration config) {
    Config.AutoRestart autoRestart = new Config.AutoRestart(config);
    this.matchLimit =
        autoRestart.enabled() && autoRestart.matchLimit() > 0 ? autoRestart.matchLimit() : null;
    this.defaultCountdownTime = autoRestart.time();
  }

  @EventHandler
  public void loadConfig(ConfigLoadEvent event) {
    this.loadConfig(event.getConfig());
  }

  /** Start a countdown with the default duration if there isn't already one running */
  private void ensureCountdown(Match match) {
    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    ctx.cancelOthers(RestartCountdown.class);
    if (ctx.getCountdown() == null) {
      this.logger.info("STARTING default countdown");
      ctx.start(new RestartCountdown(match), this.defaultCountdownTime);
    }
  }

  /** Start a countdown of the given duration after cancelling any existing one */
  private void startCountdown(Match match, Duration duration) {
    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    ctx.cancelAll();
    this.logger.info("STARTING countdown from " + duration);
    ctx.start(new RestartCountdown(match), duration);
  }

  private void cancelCountdown(Match match) {
    SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
    if (ctx.getCountdown(RestartCountdown.class) != null) {
      this.logger.info("Cancelling countdown");
      ctx.cancelAll();
    }
  }

  private void checkRestart(Match match) {
    if (this.deferral == null) return;

    if (match.isRunning()) {
      if (match.getParticipants().isEmpty()) {
        this.logger.info("Ending empty match due to restart request");
        match.finish();
      }
    } else {
      this.ensureCountdown(match);
    }
  }

  /**
   * When a restart is requested, let it restart immediately if the server if empty, otherwise defer
   * the restart and ensure that a countdown is running.
   */
  @EventHandler
  public void onRequestRestart(RequestRestartEvent event) {
    if (!this.plugin.getServer().getOnlinePlayers().isEmpty()) {
      // FIXME: Fix for multi-match support
      Match match = PGM.get().getMatchManager().getMatches().iterator().next();
      if (match == null) return;

      this.logger.info("Deferring restart");
      this.deferral = event.defer(this.plugin);
      this.checkRestart(match);
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
      this.checkRestart(event.getMatch());
    } else if (this.matchLimit != null && this.matchCount >= this.matchLimit) {
      RestartManager.get()
          .requestRestart(
              "Reached match limit (" + this.matchCount + " >= " + this.matchLimit + ")");
    }
  }

  /** If the match empties out while a restart is queued, finish the match */
  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    this.checkRestart(event.getMatch());
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
   * Request a restart from {@link RestartManager} and defer it with a countdown of the given
   * duration.
   */
  public void queueRestart(Match match, Duration duration, String reason) {
    RestartManager.get().requestTimedRestart(duration);
  }

  /**
   * Set the match limit restart to the given number of matches from now, or disable the limit if
   * given null;
   *
   * @return The number of matches from now when the server will restart
   */
  public @Nullable Integer restartAfterMatches(@Nullable Integer matches) {
    this.matchLimit = matches == null ? null : this.matchCount + matches;
    return this.matchLimit == null ? null : this.matchLimit - this.matchCount;
  }

  private class RestartCountdown extends MatchCountdown {
    public RestartCountdown(Match match) {
      super(match);
    }

    @Override
    protected Component formatText() {
      if (remaining.isLongerThan(Duration.ZERO)) {
        return new PersonalizedText(
            new PersonalizedTranslatable(
                "broadcast.serverRestart.message", secondsRemaining(ChatColor.DARK_RED)),
            ChatColor.AQUA);
      } else {
        return new PersonalizedText(
            new PersonalizedTranslatable("broadcast.serverRestart.kickMsg"), ChatColor.RED);
      }
    }

    @Override
    public void onCancel(Duration remaining, Duration total) {
      super.onCancel(remaining, total);
      if (RestartManager.get().isRestartRequested()) {
        logger.info("Cancelling restart because countdown was cancelled");
        RestartManager.get().cancelRestart();
      }
    }

    @Override
    public void onEnd(Duration total) {
      super.onEnd(total);
      if (deferral != null) {
        logger.info("Resuming restart after countdown");
        deferral.resume();
        deferral = null;
      }
    }
  }
}
