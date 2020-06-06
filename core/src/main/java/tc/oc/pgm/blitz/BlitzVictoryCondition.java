package tc.oc.pgm.blitz;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.VictoryCondition;

public class BlitzVictoryCondition implements VictoryCondition {

  @Override
  public Priority getPriority() {
    return Priority.BLITZ;
  }

  @Override
  public boolean isFinal(Match match) {
    return false;
  }

  @Override
  public boolean isCompleted(Match match) {
    int count = 0;
    for (Competitor competitor : match.getCompetitors()) {
      if (!competitor.getPlayers().isEmpty()) {
        if (++count >= 2) return false;
      }
    }
    return true;
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    return Integer.compare(b.getPlayers().size(), a.getPlayers().size());
  }

  @Override
  public Component getDescription(Match match) {
    return TranslatableComponent.of("victoryCondition.blitz");
  }
}
