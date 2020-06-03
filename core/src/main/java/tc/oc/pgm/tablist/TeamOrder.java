package tc.oc.pgm.tablist;

import java.util.Comparator;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.Teams;

public class TeamOrder implements Comparator<Team> {

  private final MatchPlayer viewer;

  public TeamOrder(MatchPlayer viewer) {
    this.viewer = viewer;
  }

  public MatchPlayer getViewer() {
    return viewer;
  }

  @Override
  public int compare(Team a, Team b) {
    if (a == b) return 0;

    // Observing team is last
    boolean aObs = a.isObserving();
    boolean bObs = b.isObserving();
    if (aObs && !bObs) return 1;
    if (bObs && !aObs) return -1;

    // Viewer's (participating) team is first
    Team team = Teams.get(viewer);
    if (team == a) return -1;
    if (team == b) return 1;

    // Rest of the teams are ordered by name
    return a.getNameLegacy().compareTo(b.getNameLegacy());
  }
}
