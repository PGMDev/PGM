package tc.oc.pgm.score;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;

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
    return TranslatableComponent.of("victoryCondition.score");
  }
}
