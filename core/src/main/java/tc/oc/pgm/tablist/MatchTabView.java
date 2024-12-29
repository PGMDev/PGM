package tc.oc.pgm.tablist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
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

    if (this.match != null && dirtyTracker.isLayout()) {
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

        int y1 = this.getHeader();
        Iterator<Team> teamIt = teams.iterator();
        while (teamIt.hasNext()) {
          int y2 = y1;
          for (int x1 = 0; x1 < getWidth(); ) {
            if (!teamIt.hasNext()) {
              fillEmpty(x1, this.getWidth(), y1, y2);
              break;
            }

            Team team = teamIt.next();
            int columnsForTeam = getColumnsForTeam(team, teams);
            int currY2 = participantRows + getHeader(); // Default to max height
            // Size tightly vertically when teams don't use multiple columns
            if (columnsForTeam == 1) currY2 = Math.min(y1 + team.getPlayers().size() + 2, currY2);

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
                  x1 + columnsForTeam,
                  y1,
                  y2);
            }

            x1 += columnsForTeam;
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
    int teamsPerColumn = Math.min(this.getWidth(), teams.size());

    int biggestTeamColumn = 0;
    Iterator<Team> teamIt = teams.iterator();
    while (teamIt.hasNext()) {
      int mostRows = 0;
      for (int x = 0; x < teamsPerColumn && teamIt.hasNext(); x++) {
        Team team = teamIt.next();
        mostRows = Math.max(
            mostRows, divideRoundingUp(team.getPlayers().size(), getColumnsForTeam(team, teams)));
      }

      biggestTeamColumn += 2 + mostRows;
    }
    return biggestTeamColumn;
  }

  private int getColumnsForTeam(Team team, Collection<Team> teams) {
    if (teams.size() < getWidth()) {
      float cols = (float) team.getMaxPlayers() * getWidth() / match.getMaxPlayers();
      if (cols % 1 == 0.5 && cols > ((float) getWidth() / teams.size())) cols -= 0.5f;
      return Math.max(1, Math.min(Math.round(cols), getWidth() - teams.size() + 1));
    } else {
      return 1;
    }
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

      dirtyTracker.invalidateLayout();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeamChange(PlayerPartyChangeEvent event) {
    if (this.match != event.getMatch()) return;

    updatePlayerParty(event.getPlayer(), event.getOldParty(), event.getNewParty());

    // Your own view should re-render quickly after join/leave
    if (this.matchPlayer == event.getPlayer()) dirtyTracker.prioritize();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVanish(PlayerVanishEvent event) {
    updatePlayerParty(
        event.getPlayer(), event.getPlayer().getParty(), event.getPlayer().getParty());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerNameChange(NameDecorationChangeEvent event) {
    MatchPlayer mp = match.getPlayer(event.getUUID());
    if (mp != null) updatePlayerParty(mp, mp.getParty(), mp.getParty());
  }

  private void updatePlayerParty(
      MatchPlayer player, @Nullable Party oldParty, @Nullable Party newParty) {
    if (oldParty != null) {
      this.participantPlayers.remove(player);
      this.observerPlayers.remove(player);

      if (oldParty instanceof Team) this.teamPlayers.get((Team) oldParty).remove(player);
    }

    if (newParty != null && !shouldHide(player)) {
      List<MatchPlayer> players =
          newParty instanceof Competitor ? participantPlayers : observerPlayers;

      if (!players.contains(player)) players.add(player);

      if (newParty instanceof Team && !this.teamPlayers.containsEntry((Team) newParty, player))
        this.teamPlayers.put((Team) newParty, player);
    }

    dirtyTracker.invalidateLayout();
  }

  private boolean shouldHide(MatchPlayer other) {
    return other != matchPlayer
        && Integration.isVanished(other.getBukkit())
        && !matchPlayer.getBukkit().hasPermission(Permissions.VANISH);
  }

  private static int divideRoundingUp(int numerator, int denominator) {
    return (numerator + denominator - 1) / denominator;
  }
}
