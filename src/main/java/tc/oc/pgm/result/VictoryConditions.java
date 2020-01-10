package tc.oc.pgm.result;

import javax.annotation.Nullable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.goals.GoalsVictoryCondition;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamVictoryCondition;
import tc.oc.pgm.teams.Teams;

public class VictoryConditions {
  private VictoryConditions() {}

  public static @Nullable VictoryCondition parse(MapContext context, String raw) {
    return parse(null, context, raw);
  }

  public static @Nullable VictoryCondition parse(Match match, String raw) {
    return parse(match, null, raw);
  }

  private static @Nullable VictoryCondition parse(
      Match match, MapContext context, @Nullable String raw) {
    if (raw == null) return null;

    switch (raw.toLowerCase()) {
      case "default":
        return null;
      case "tie":
        return new TieVictoryCondition();
      case "objectives":
        return new GoalsVictoryCondition();
      default:
        if (match != null) {
          Team winner = Teams.getTeam(raw, match);
          if (winner == null) {
            throw new IllegalArgumentException("Invalid result");
          }
          return new TeamVictoryCondition(winner.getDefinition());
        } else {
          TeamFactory winner = Teams.getTeam(raw, context);
          if (winner == null) {
            throw new IllegalArgumentException("Invalid result");
          }
          return new TeamVictoryCondition(winner);
        }
    }
  }
}
