package tc.oc.pgm.cycle;

import javax.annotation.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.joda.time.Duration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapOrder;

@ListenerScope(MatchScope.LOADED)
public class CycleMatchModule implements MatchModule, Listener {

  private final MapOrder mapOrder;
  private final Match match;
  private final CycleConfig config;

  public CycleMatchModule(Match match) {
    this.match = match;
    this.mapOrder = PGM.get().getMapOrder();
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
    match.finish();
    match.getCountdown().start(new CycleCountdown(match), duration);
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (match.isRunning() && match.getParticipants().isEmpty()) {
      CycleConfig.Auto autoConfig = config.matchEmpty();
      if (autoConfig.enabled()) {
        match.getLogger().info("Cycling due to empty match");
        startCountdown(autoConfig.countdown());
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    final Match match = event.getMatch();
    mapOrder.matchEnded(match);

    if (!RestartManager.isQueued()) {
      CycleConfig.Auto autoConfig = config.matchEnd();
      if (autoConfig.enabled()) {
        startCountdown(autoConfig.countdown());
      }
    }
  }
}
