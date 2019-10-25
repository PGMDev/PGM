package tc.oc.pgm.tablist;

import java.util.Comparator;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.Parties;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.Teams;

public class TeamOrder implements Comparator<Team> {

  private final MatchPlayer viewer;

  TeamOrder(MatchPlayer viewer) {
    this.viewer = viewer;
  }

  @Override
  public int compare(Team a, Team b) {
    if (a == b) return 0;

    // Observing team is last
    boolean aObs = Parties.isObservingType(a);
    boolean bObs = Parties.isObservingType(b);
    if (aObs && !bObs) return 1;
    if (bObs && !aObs) return -1;

    // Viewer's (participating) team is first
    Team team = Teams.get(viewer);
    if (team == a) return -1;
    if (team == b) return 1;

    // Rest of the teams are ordered by name
    return a.getName().compareTo(b.getName());
  }
}
