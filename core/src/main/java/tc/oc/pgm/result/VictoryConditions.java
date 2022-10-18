package tc.oc.pgm.result;

import static tc.oc.pgm.util.text.TextException.invalidFormat;

import java.util.Optional;
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
    return parse(null, factory, raw);
  }

  public static @NotNull Optional<VictoryCondition> parseOptional(
      Match match, @Nullable String raw) {
    return Optional.ofNullable(parse(match, null, raw));
  }

  public static @Nullable VictoryCondition parse(
      Match match, MapFactory factory, @Nullable String raw) {
    if (raw == null) return null;

    switch (raw.toLowerCase()) {
      case "default":
        return null;
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
