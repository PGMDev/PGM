package tc.oc.pgm.cycle;

import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.joda.time.Duration;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.MatchEndEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.match.*;
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
    this.mm = match.getPlugin().matchManager;
    this.config = new CycleConfig(match.getPlugin().getConfig());
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

  public void startCountdown() {
    startCountdown((Duration) null);
  }

  public void startCountdown(PGMMap nextMap) {
    startCountdown(null, nextMap);
  }

  public void startCountdown(@Nullable Duration duration) {
    if (duration == null) duration = config.countdown();
    getMatch().end();
    if (Duration.ZERO.equals(duration)) {
      mm.cycle(getMatch(), false, false);
    } else {
      getMatch().getCountdownContext().start(new CycleCountdown(mm, getMatch()), duration);
    }
  }

  public void startCountdown(@Nullable Duration duration, PGMMap nextMap) {
    mm.setNextMap(nextMap);
    startCountdown(duration);
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    final Match match = getMatch();
    if (match.isRunning() && match.getParticipatingPlayers().isEmpty()) {
      CycleConfig.Auto autoConfig = config.matchEmpty();
      if (autoConfig.enabled()) {
        logger.info("Cycling due to empty match");
        startCountdown(autoConfig.countdown());
      }
    }
  }

  @EventHandler
  public void onMatchEnd(MatchEndEvent event) {
    final Match match = event.getMatch();
    if (!RestartManager.get().isRestartRequested()) {
      CycleConfig.Auto autoConfig = config.matchEnd();
      if (autoConfig.enabled()) {
        startCountdown(autoConfig.countdown());
      }
    }
  }
}
