package tc.oc.pgm.score;

import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.result.VictoryCondition;

public class ScoreVictoryCondition implements VictoryCondition {

  @Override
  public Priority getPriority() {
    return Priority.SCORE;
  }

  @Override
  public boolean isFinal(Match match) {
    return false;
  }

  @Override
  public boolean isCompleted(Match match) {
    ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
    if (!smm.hasScoreLimit()) return false;

    double limit = smm.getScoreLimit();
    for (Competitor competitor : match.getCompetitors()) {
      if (smm.getScore(competitor) >= limit) return true;
    }
    return false;
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    ScoreMatchModule smm = a.getMatch().needModule(ScoreMatchModule.class);
    return Double.compare(smm.getScore(b), smm.getScore(a)); // reversed
  }

  @Override
  public Component getDescription(Match match) {
    return new PersonalizedText("highest score");
  }
}
