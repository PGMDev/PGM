package tc.oc.pgm.result;

import static com.google.common.base.Preconditions.checkNotNull;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

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
    return TranslatableComponent.of(
        competitor.isNamePlural()
            ? "broadcast.gameOver.teamWinners"
            : "broadcast.gameOver.teamWinner",
        competitor.getName());
  }
}
