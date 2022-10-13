package tc.oc.pgm.result;

import static tc.oc.pgm.util.text.TextException.invalidFormat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;
import tc.oc.pgm.goals.GoalsVictoryCondition;
import tc.oc.pgm.score.ScoreVictoryCondition;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamVictoryCondition;
import tc.oc.pgm.teams.Teams;

public class VictoryConditions {

  private VictoryConditions() {}

  public static @Nullable VictoryCondition parseNullable(MapFactory factory, @Nullable String raw) {
    VictoryCondition vc = parseNotNull(null, factory, raw);
    return vc == NullVictoryCondition.INSTANCE ? null : vc;
  }

  public static @NotNull VictoryCondition parseNotNull(Match match, @Nullable String raw) {
    return parseNotNull(match, null, raw);
  }

  public static @NotNull VictoryCondition parseNotNull(
      Match match, MapFactory factory, @Nullable String raw) {
    if (raw == null) return NullVictoryCondition.INSTANCE;

    switch (raw.toLowerCase()) {
      case "default":
        return NullVictoryCondition.INSTANCE;
      case "tie":
        return new TieVictoryCondition();
      case "objectives":
        return new GoalsVictoryCondition();
      case "score":
        return new ScoreVictoryCondition();
      default:
        TeamFactory winner;
        if (match != null) winner = Teams.getTeam(raw, match);
        else winner = Teams.getTeam(raw, factory);
        if (winner == null) throw invalidFormat(raw, Team.class, null);
        return new TeamVictoryCondition(winner);
    }
  }
}
