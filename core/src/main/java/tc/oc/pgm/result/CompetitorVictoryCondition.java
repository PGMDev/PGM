package tc.oc.pgm.result;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;

/** Immediate, unconditional victory for an explicit {@link Competitor} */
public class CompetitorVictoryCondition extends ImmediateVictoryCondition {

  private final Competitor competitor;

  public CompetitorVictoryCondition(Competitor competitor) {
    this.competitor = checkNotNull(competitor);
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    return Boolean.compare(b == competitor, a == competitor);
  }

  @Override
  public Component getDescription(Match match) {
    return new PersonalizedTranslatable(
        competitor.isNamePlural()
            ? "broadcast.gameOver.teamWinners"
            : "broadcast.gameOver.teamWinner",
        competitor.getComponentName());
  }
}
