package tc.oc.pgm.cycle;

import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joda.time.Duration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.restart.RestartManager;

@ModuleDescription(name = "Cycle")
@ListenerScope(MatchScope.LOADED)
public class CycleMatchModule extends MatchModule implements Listener {
  public static class Factory implements MatchModuleFactory<CycleMatchModule> {
    @Override
    public CycleMatchModule createMatchModule(Match match) throws ModuleLoadException {
      return new CycleMatchModule(match);
    }
  }

  private final MatchManager mm;
  private final CycleConfig config;

  public CycleMatchModule(Match match) {
    super(match);
    this.mm = PGM.get().getMatchManager();
    this.config = new CycleConfig(PGM.get().getConfig());
  }

  public CycleConfig getConfig() {
    return config;
  }

  public void cycleNow() {
    startCountdown(Duration.ZERO);
  }

  public void startCountdown(@Nullable Duration duration) {
    if (duration == null) duration = config.countdown();
    getMatch().finish();
    if (Duration.ZERO.equals(duration)) {
      mm.cycleMatch(getMatch(), mm.getMapOrder().popNextMap(), false);
    } else {
      getMatch().getCountdown().start(new CycleCountdown(mm, getMatch()), duration);
    }
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    final Match match = getMatch();
    if (match.isRunning() && match.getParticipants().isEmpty()) {
      CycleConfig.Auto autoConfig = config.matchEmpty();
      if (autoConfig.enabled()) {
        logger.info("Cycling due to empty match");
        startCountdown(autoConfig.countdown());
      }
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    final Match match = event.getMatch();
    mm.getMapOrder().matchEnded(match);

    if (!RestartManager.isQueued()) {
      CycleConfig.Auto autoConfig = config.matchEnd();
      if (autoConfig.enabled()) {
        startCountdown(autoConfig.countdown());
      }
    }
  }
}
