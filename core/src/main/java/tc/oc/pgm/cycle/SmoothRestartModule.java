package tc.oc.pgm.cycle;

import static net.kyori.adventure.text.Component.text;

import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;

@ListenerScope(MatchScope.LOADED)
public class SmoothRestartModule implements MatchModule, Listener {

  private final Match match;
  private FakeCycleCountdown countdown;
  private MapPoll currentPoll;

  public SmoothRestartModule(Match match) {
    this.match = match;
  }

  public void updateSelectedMap(MapInfo nextMap) {
    if (nextMap == null) return;
    Bukkit.broadcastMessage(nextMap.toString());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onRequestRestart(RequestRestartEvent event) {
    // Randomly allow for regular restarts
    if (match.getRandom().nextBoolean()) {
      match.sendMessage(text("real restart"));
      return;
    }

    match.sendMessage(text("fake cycle"));

    countdown = new FakeCycleCountdown(match, this);
    event.setRestartCountdown(countdown);
  }

  public void tryStartCycleCountdown() {
    countdown = new FakeCycleCountdown(match, this);
    match.getCountdown().start(countdown, PGM.get().getConfiguration().getCycleTime());
  }

  @EventHandler
  public void onRestartCancel(CancelRestartEvent event) {
    if (match.getCountdown().isRunning(countdown)) {
      match.getCountdown().cancel(countdown);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountdownStart(CountdownStartEvent event) {
    if (!(event.getCountdown() instanceof FakeCycleCountdown)) return;

    //    // TODO: check that we are on a voted pool
    MapOrder mapOrder = PGM.get().getMapOrder();

    MapInfo nextMap = mapOrder.getNextMap();
    if (nextMap != null) Bukkit.broadcastMessage("next map: " + nextMap.getName());

    if (mapOrder instanceof MapPoolManager) {

      MapPool pool = ((MapPoolManager) mapOrder).getActiveMapPool();

      if (pool instanceof VotingPool votingPool) {

        match
            .getExecutor(MatchScope.LOADED)
            .schedule(
                () -> {
                  votingPool.startMapPoll(match);
                },
                5,
                TimeUnit.SECONDS);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountdownEnd(CountdownEndEvent event) {
    if (!(event.getCountdown() instanceof FakeCycleCountdown)) return;

    Bukkit.getServer().broadcastMessage("next map: " + countdown.nextMap);

    event.getMatch().sendMessage(text("get the map"));

    // TODO: how to actual restart?
    // RestartListener.getInstance().startRestartCountdown(event.getMatch());
  }
}
