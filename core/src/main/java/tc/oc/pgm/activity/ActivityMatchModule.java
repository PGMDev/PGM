package tc.oc.pgm.activity;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.events.PlayerLeavePartyEvent;

@ListenerScope(MatchScope.LOADED)
public class ActivityMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, PlayerActivityTracker> trackers = Maps.newHashMap();

  public ActivityMatchModule(Match match) {
    this.match = match;
  }

  public Map<UUID, PlayerActivityTracker> getActivityLogs() {
    return trackers;
  }

  public PlayerActivityTracker getPlayerActivity(UUID playerId) {
    return trackers.computeIfAbsent(playerId, k -> new PlayerActivityTracker());
  }

  @EventHandler
  public void onMatchStart(final MatchStartEvent event) {
    event
        .getMatch()
        .getParticipants()
        .forEach(player -> getPlayerActivity(player.getId()).start(player.getCompetitor()));
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMatchEnd(final MatchFinishEvent event) {
    trackers.forEach((uuid, playerMatchTracker) -> playerMatchTracker.endAll());
  }

  @EventHandler
  public void onPlayerJoinMatch(final PlayerJoinPartyEvent event) {
    // Only modify trackers when match is running
    if (!event.getMatch().isRunning()) return;

    // End time tracking for old party
    if (event.getOldParty() instanceof Competitor) {
      getPlayerActivity(event.getPlayer().getId()).end((Competitor) event.getOldParty());
    }

    // When joining a party that's playing, start time tracking
    if (event.getNewParty() instanceof Competitor) {
      getPlayerActivity(event.getPlayer().getId()).start((Competitor) event.getNewParty());
    }
  }

  @EventHandler
  public void onPlayerLeaveMatch(final PlayerLeavePartyEvent event) {
    if (event.getMatch().isRunning() && event.getParty() instanceof Competitor) {
      getPlayerActivity(event.getPlayer().getId()).end((Competitor) event.getParty());
    }
  }
}
