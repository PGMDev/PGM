package tc.oc.pgm.tablist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.match.ObservingParty;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.tablist.TabEntry;
import tc.oc.pgm.util.tablist.TabView;

public class MatchTabView extends TabView implements Listener {

  private final List<MatchPlayer> observerPlayers = new ArrayList<>();
  private final List<MatchPlayer> participantPlayers = new ArrayList<>();
  private final List<Team> teams = new ArrayList<>();
  private final ListMultimap<Team, MatchPlayer> teamPlayers = ArrayListMultimap.create();

  private Match match;
  private @Nullable TeamMatchModule tmm;
  private MatchPlayer matchPlayer;
  private PlayerOrder playerOrder;
  private TeamOrder teamOrder;

  public MatchTabView(Player viewer) {
    super(viewer); // All fields must be reset in PlayerJoinMatchEvent to prevent leaks
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
      boolean footer,
      boolean vertical,
      final int x1,
      final int x2,
      int y1,
      int y2) {

    if (footer && y2 < getHeight()) { // Render an empty row underneath
      y2 -= 1;
      for (int x = x1; x < x2; x++) this.setSlot(x, y2, null);
    }

    if (header != null && y1 < y2) { // Render the header row
      for (int x = x1; x < x2; x++) this.setSlot(x, y1, x == x1 ? header : null);
      y1 += 1;
    }

    // Re-sort team members and render them
    players.sort(this.playerOrder);
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

  private void fillEmpty(final int x1, final int x2, final int y1, final int y2) {
    for (int x = x1; x < x2; x++) {
      for (int y = y1; y < y2; y++) {
        this.setSlot(x, y, null);
      }
    }
  }

  public int getHeader() {
    return display != null ? 2 : 0;
  }

  @Override
  public void render() {
    if (this.manager == null) return;

    if (this.match != null && this.isLayoutDirty()) {
      if (display == null) {
        this.setHeader(this.getManager().getMapEntry(this.match));
        this.setFooter(this.getManager().getFooterEntry(this.match));
      }

      // Number of players/staff on observers
      int observingPlayers = 0;
      int observingStaff = 0;
      if (PGM.get().getConfiguration().canParticipantsSeeObservers() || matchPlayer.isObserving()) {
        observingPlayers = observerPlayers.size();
        for (MatchPlayer player : observerPlayers) {
          if (player.getBukkit().hasPermission(Permissions.STAFF)) observingStaff++;
        }
      }

      int availableRows = this.getHeight() - this.getHeader();
      int observerRows;

      if (tmm != null) {
        teams.sort(teamOrder);

        int participantRows = getMinimumParticipantRows(teams);
        observerRows = getObserverRows(observingPlayers, observingStaff, participantRows);
        participantRows = availableRows - observerRows;

        int columnsPerTeam = Math.max(1, this.getWidth() / Math.max(1, teams.size()));

        int y1 = this.getHeader();
        Iterator<Team> teamIt = teams.iterator();
        while (teamIt.hasNext()) {
          int y2 = y1;
          for (int x1 = 0; x1 < getWidth(); x1 += columnsPerTeam) {
            if (!teamIt.hasNext()) {
              fillEmpty(x1, x1 + columnsPerTeam, y1, y2);
              continue;
            }

            Team team = teamIt.next();
            int currY2 = participantRows + getHeader(); // Default to max height
            // Size tightly vertically when teams don't use multiple columns
            if (columnsPerTeam == 1) currY2 = Math.min(y1 + team.getPlayers().size() + 2, currY2);

            if (currY2 > y2) {
              // If the max y on this row of teams increases, fill the void under previous teams
              fillEmpty(0, x1, y2, currY2);
              y2 = currY2;
            }

            if (y2 > y1) { // At the very least one row will render
              renderTeam(
                  teamPlayers.get(team),
                  getManager().getTeamEntry(team),
                  true,
                  true,
                  x1,
                  x1 + columnsPerTeam,
                  y1,
                  y2);
            }
          }

          y1 = y2;
        }

        // Clear any leftover empty space after teams
        fillEmpty(0, getWidth(), y1, participantRows);
      } else {
        int participantRows = 2 + divideRoundingUp(participantPlayers.size(), this.getWidth());
        observerRows = getObserverRows(observingPlayers, observingStaff, participantRows);
        participantRows = availableRows - observerRows;

        this.renderTeam(
            participantPlayers,
            getManager().getFreeForAllEntry(match),
            true,
            true,
            0,
            this.getWidth(),
            getHeader(),
            participantRows + getHeader());
      }

      if (observerRows > 0) {
        // Render observers
        this.renderTeam(
            observerPlayers,
            null,
            false,
            false,
            0,
            this.getWidth(),
            this.getHeight() - observerRows,
            this.getHeight());
      }

      if (getHeader() > 0) {
        TabEntry[] header = this.getManager().getHeaderEntries(match);
        for (int i = 0; i < getWidth(); i++) {
          setSlot(i, 0, i < header.length ? header[i] : null);
        }

        fillEmpty(0, getWidth(), 1, getHeader());
      }
    }

    super.render();
  }

  private int getMinimumParticipantRows(Collection<Team> teams) {
    int columnsPerTeam = Math.max(1, this.getWidth() / Math.max(1, teams.size()));
    int teamsPerColumn = Math.min(this.getWidth(), teams.size());

    int biggestTeamColumn = 0;
    Iterator<Team> teamIt = teams.iterator();
    while (teamIt.hasNext()) {
      int biggestTeam = 0;
      for (int x = 0; x < teamsPerColumn && teamIt.hasNext(); x++)
        biggestTeam = Math.max(biggestTeam, teamIt.next().getPlayers().size());

      biggestTeamColumn += 2 + divideRoundingUp(biggestTeam, columnsPerTeam);
    }
    return biggestTeamColumn;
  }

  private int getObserverRows(int observers, int observingStaff, int participantRows) {
    int obsRows = getHeight() - getHeader() - participantRows;
    obsRows = Math.min(divideRoundingUp(observers, this.getWidth()), obsRows);
    obsRows = Math.max(obsRows, divideRoundingUp(observingStaff, this.getWidth()));
    return obsRows;
  }

  public void onViewerJoinMatch(PlayerJoinMatchEvent event) {
    if (this.getViewer() == event.getPlayer().getBukkit()) {
      this.match = event.getMatch();
      this.matchPlayer = event.getPlayer();

      this.playerOrder = new PlayerOrder(this.matchPlayer);
      this.teamOrder = new TeamOrder(this.matchPlayer);

      this.observerPlayers.clear();
      this.observerPlayers.addAll(this.match.getObservers());
      this.observerPlayers.removeIf(this::shouldHide);
      this.participantPlayers.clear();
      this.participantPlayers.addAll(this.match.getParticipants());

      this.teams.clear();
      this.teamPlayers.clear();
      this.tmm = this.match.getModule(TeamMatchModule.class);
      if (this.tmm != null) {
        for (Team team : this.tmm.getParticipatingTeams()) {
          this.teams.add(team);
          this.teamPlayers.putAll(team, team.getPlayers());
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

    if (event.getNewParty() != null && !shouldHide(event.getPlayer())) {
      List<MatchPlayer> players =
          event.getNewParty() instanceof ObservingParty ? observerPlayers : participantPlayers;

      if (!players.contains(event.getPlayer())) players.add(event.getPlayer());
    }

    if (event.getOldParty() instanceof Team) {
      this.teamPlayers.get((Team) event.getOldParty()).remove(event.getPlayer());
    }

    if (event.getNewParty() instanceof Team
        && !this.teamPlayers.containsEntry((Team) event.getNewParty(), event.getPlayer())) {
      this.teamPlayers.put((Team) event.getNewParty(), event.getPlayer());
    }

    this.invalidateLayout();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVanish(PlayerVanishEvent event) {
    if (event.isVanished()) {
      if (shouldHide(event.getPlayer())) this.observerPlayers.remove(event.getPlayer());
    } else if (!this.observerPlayers.contains(event.getPlayer())) {
      this.observerPlayers.add(event.getPlayer());
    }

    this.invalidateLayout();
  }

  private boolean shouldHide(MatchPlayer other) {
    return other != matchPlayer
        && other.isVanished()
        && !matchPlayer.getBukkit().hasPermission(Permissions.STAFF);
  }

  private static int divideRoundingUp(int numerator, int denominator) {
    return (numerator + denominator - 1) / denominator;
  }
}
