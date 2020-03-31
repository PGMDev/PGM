package tc.oc.pgm.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.teams.Team;

@ListenerScope(MatchScope.LOADED)
public class TeamRestrictMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<UUID, Party> playerTeamMap = new HashMap<>();

  public TeamRestrictMatchModule(Match match) {
    this.match = match;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void addPlayerToMatch(PlayerJoinPartyEvent event) {
    UUID playerID = event.getPlayer().getId();
    if (playerTeamMap.containsKey(playerID)
        && !event
            .getNewParty()
            .isObserving()) { // If player was previously on team but joins obs, keep previous team
      playerTeamMap.replace(playerID, event.getNewParty());

    } else if (!playerTeamMap.containsKey(playerID)) {
      playerTeamMap.put(playerID, event.getNewParty());
    }
  }

  public Map<UUID, Party> getPlayerTeamMap() {
    return playerTeamMap;
  }

  public Team getLastTeam(UUID id) {
    Party lastParty = playerTeamMap.get(id);
    if (lastParty instanceof Team) {
      return (Team) lastParty;
    } else {
      return null;
    }
  }
}
