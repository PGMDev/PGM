package tc.oc.pgm.tablist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.*;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.identity.PlayerIdentityChangeEvent;
import tc.oc.pgm.Config;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.tablist.TabEntry;
import tc.oc.tablist.TabView;
import tc.oc.util.Numbers;
import tc.oc.util.collection.DefaultProvider;

public class MatchTabView extends TabView implements Listener {

  public static class Factory implements DefaultProvider<Player, MatchTabView> {
    @Override
    public MatchTabView get(Player key) {
      return new MatchTabView(key);
    }
  }

  private final List<MatchPlayer> observerPlayers = new ArrayList<>();
  private final List<MatchPlayer> participantPlayers = new ArrayList<>();
  private final ListMultimap<Team, MatchPlayer> teamPlayers = ArrayListMultimap.create();

  private Match match;
  private @Nullable TeamMatchModule tmm;
  private MatchPlayer matchPlayer;
  private PlayerOrder playerOrder;
  private TeamOrder teamOrder;

  public MatchTabView(Player viewer) {
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

  private void renderTeam(
      List<MatchPlayer> players,
      @Nullable TabEntry header,
      boolean vertical,
      final int x1,
      final int x2,
      int y1,
      final int y2) {
    if (header != null) {
      // Render the header row
      for (int x = x1; x < x2; x++) {
        this.setSlot(x, y1, x == x1 ? header : null);
      }
      y1++;
    }

    // Re-sort team members and render them
    Collections.sort(players, this.playerOrder);
    Iterator<MatchPlayer> iter = players.iterator();

    if (vertical) {
      // Fill columns first
      for (int x = x1; x < x2; x++) {
        for (int y = y1; y < y2; y++) {
          this.setSlot(x, y, iter.hasNext() ? this.getManager().getPlayerEntry(iter.next()) : null);
        }
      }
    } else {
      // Fill rows first
      for (int y = y1; y < y2; y++) {
        for (int x = x1; x < x2; x++) {
          this.setSlot(x, y, iter.hasNext() ? this.getManager().getPlayerEntry(iter.next()) : null);
        }
      }
    }
  }

  @Override
  public void render() {
    if (this.manager == null) return;

    if (this.match != null && this.isLayoutDirty()) {
      this.setHeader(this.getManager().getMapEntry(this.match));
      this.setFooter(this.getManager().getFooterEntry(this.match));

      Collection<MatchPlayer> observers = this.match.getObservers();

      // Number of players/staff on observers
      int observingPlayers = 0;
      int observingStaff = 0;
      if (Config.PlayerList.playersSeeObservers() || matchPlayer.isObserving()) {
        observingPlayers = observers.size();
        for (MatchPlayer player : observers) {
          if (player.getBukkit().hasPermission(Permissions.STAFF)) observingStaff++;
        }
      }

      int availableRows = this.getHeight();

      // Minimum rows required to show all staff observers, including a blank row
      int observerRows =
          Math.min(availableRows, 1 + Numbers.divideRoundingUp(observingStaff, this.getWidth()));

      if (tmm != null) {
        // Size of the largest team
        int maxTeamSize = 0;
        for (Team team : tmm.getParticipatingTeams()) {
          maxTeamSize = Math.max(maxTeamSize, team.getPlayers().size());
        }

        int columnsPerTeam =
            Math.max(1, this.getWidth() / Math.max(1, tmm.getParticipatingTeams().size()));
        ;

        // Minimum rows required by teams (when they are distributed evenly across columns),
        // including the header row
        int teamRows =
            Math.min(
                availableRows - observerRows,
                1 + Numbers.divideRoundingUp(maxTeamSize, columnsPerTeam));

        // Expand observer rows until all observers are showing
        observerRows =
            Math.min(
                availableRows - teamRows,
                1 + Numbers.divideRoundingUp(observingPlayers, this.getWidth()));

        // If there is somehow only one observer row, it's only the blank row, so it might as well
        // be zero
        if (observerRows == 1) observerRows = 0;

        // Expand team rows to fill whatever if left
        teamRows = availableRows - observerRows;

        // Render participating teams
        List<Team> teams = new ArrayList<>(tmm.getParticipatingTeams());
        Collections.sort(teams, this.teamOrder);
        Iterator<Team> iter = teams.iterator();

        for (int x1 = 0; x1 < this.getWidth(); ) {
          if (iter.hasNext()) {
            Team team = iter.next();
            int x2 = Math.min(x1 + columnsPerTeam, this.getWidth());
            this.renderTeam(
                teamPlayers.get(team), getManager().getTeamEntry(team), true, x1, x2, 0, teamRows);
            x1 = x2;
          } else {
            for (int y = 0; y < teamRows; y++) {
              this.setSlot(x1, y, null);
            }
            x1++;
          }
        }
      } else {
        // Minimum rows required by participating players
        int participantRows =
            Math.min(
                availableRows - observerRows,
                1 + Numbers.divideRoundingUp(participantPlayers.size(), this.getWidth()));

        // Expand observer rows until all observers are showing
        observerRows =
            Math.min(
                availableRows - participantRows,
                1 + Numbers.divideRoundingUp(observingPlayers, this.getWidth()));

        // Expand participant rows to fill whatever if left
        participantRows = availableRows - observerRows;

        this.renderTeam(
            participantPlayers,
            getManager().getFreeForAllEntry(match),
            true,
            0,
            this.getWidth(),
            0,
            participantRows);
      }

      if (observerRows > 0) {
        // Render blank row between teams and observers
        for (int x = 0; x < this.getWidth(); x++) {
          this.setSlot(x, this.getHeight() - observerRows, null);
        }

        // Render observers
        this.renderTeam(
            observerPlayers,
            null,
            false,
            0,
            this.getWidth(),
            this.getHeight() - observerRows + 1,
            this.getHeight());
      }
    }

    super.render();
  }

  public void onViewerJoinMatch(PlayerJoinMatchEvent event) {
    if (this.getViewer() == event.getPlayer().getBukkit()) {
      this.match = event.getMatch();
      this.matchPlayer = event.getPlayer();

      this.playerOrder = new PlayerOrder(this.matchPlayer);
      this.teamOrder = new TeamOrder(this.matchPlayer);

      this.observerPlayers.clear();
      this.observerPlayers.addAll(this.match.getObservers());
      this.participantPlayers.clear();
      this.participantPlayers.addAll(this.match.getParticipants());

      this.tmm = this.match.getModule(TeamMatchModule.class);
      if (this.tmm != null) {
        for (Team team : this.tmm.getTeams()) {
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
