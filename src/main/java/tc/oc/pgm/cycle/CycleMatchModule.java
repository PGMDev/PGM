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
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.FixedPGMMapOrderManager;

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
    cycleNow(null);
  }

  public void cycleNow(PGMMap map) {
    startCountdown(Duration.ZERO, map);
  }

  public void startCountdown(PGMMap nextMap) {
    startCountdown(null, nextMap);
  }

  public void startCountdown(Duration duration) {
    startCountdown(duration, mm.getMapOrder().getNextMap());
  }

  public void startCountdown(@Nullable Duration duration, @Nullable PGMMap nextMap) {
    if (duration == null) duration = config.countdown();
    getMatch().finish();
    if (Duration.ZERO.equals(duration)) {
      mm.cycleMatch(getMatch(), nextMap, false);
      mm.getMapOrder().popNextMap();
    } else {
      getMatch().getCountdown().start(new CycleCountdown(mm, getMatch(), nextMap), duration);
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

    if (mm.getMapOrder() instanceof FixedPGMMapOrderManager) {
      FixedPGMMapOrderManager fixedPGMMapOrderManager = (FixedPGMMapOrderManager) mm.getMapOrder();

      /*
      Whether player counts should be evaluated for activating a more
      accurate rotation depending on the amount of online / active players or not
      */
      if (fixedPGMMapOrderManager.isEvaluatingPlayerCount()) {
        fixedPGMMapOrderManager.recalculateActiveRotation();
      }
    }

    if (!RestartManager.get().isRestartRequested()) {
      CycleConfig.Auto autoConfig = config.matchEnd();
      if (autoConfig.enabled()) {
        startCountdown(autoConfig.countdown());
      }
    }
  }
}
