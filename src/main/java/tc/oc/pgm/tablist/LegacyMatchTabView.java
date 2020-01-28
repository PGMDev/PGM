package tc.oc.pgm.tablist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.identity.PlayerIdentityChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.tablist.LegacyTabView;
import tc.oc.util.collection.DefaultProvider;

public class LegacyMatchTabView extends LegacyTabView implements Listener {

  public static class Factory implements DefaultProvider<Player, LegacyMatchTabView> {
    @Override
    public LegacyMatchTabView get(Player key) {
      return new LegacyMatchTabView(key);
    }
  }

  private final List<MatchPlayer> observerPlayers = new ArrayList<>();
  private final List<MatchPlayer> participantPlayers = new ArrayList<>();
  private final ListMultimap<Team, MatchPlayer> teamPlayers = ArrayListMultimap.create();

  private Match match;

  public LegacyMatchTabView(Player viewer) {
    super(viewer);
  }

  @Override
  public void disable() {
    HandlerList.unregisterAll(this);
    super.disable();
  }

  protected MatchTabManager getManager() {
    return (MatchTabManager) this.manager;
  }

  public void onViewerJoinMatch(PlayerJoinMatchEvent event) {
    if (this.getViewer() == event.getPlayer().getBukkit()) {
      this.match = event.getMatch();

      this.observerPlayers.clear();
      this.observerPlayers.addAll(this.match.getObservers());
      this.participantPlayers.clear();
      this.participantPlayers.addAll(this.match.getParticipants());

      TeamMatchModule tmm = this.match.getMatchModule(TeamMatchModule.class);
      if (tmm != null) {
        for (Team team : tmm.getTeams()) {
          this.teamPlayers.replaceValues(team, team.getPlayers());
        }
      }

      this.invalidateLayout();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamChange(PlayerPartyChangeEvent event) {
    if (this.match != event.getMatch()) return;

    if (event.getOldParty() != null) {
      this.participantPlayers.remove(event.getPlayer());
      this.observerPlayers.remove(event.getPlayer());
    }

    if (event.getNewParty() != null) {
      if (event.getNewParty() instanceof ObservingParty) {
        if (!this.observerPlayers.contains(event.getPlayer())) {
          this.observerPlayers.add(event.getPlayer());
        }
      } else {
        if (!this.participantPlayers.contains(event.getPlayer())) {
          this.participantPlayers.add(event.getPlayer());
        }
      }
    }

    if (event.getOldParty() instanceof Team) {
      this.teamPlayers
          .get((Team) event.getOldParty())
          .removeAll(Collections.singleton(event.getPlayer()));
    }

    if (event.getNewParty() instanceof Team
        && !this.teamPlayers.containsEntry(event.getNewParty(), event.getPlayer())) {
      this.teamPlayers.put((Team) event.getNewParty(), event.getPlayer());
    }

    this.invalidateLayout();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onNickChange(PlayerIdentityChangeEvent event) {
    this.invalidateLayout();
  }
}
