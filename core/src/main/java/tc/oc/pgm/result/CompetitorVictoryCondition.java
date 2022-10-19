package tc.oc.pgm.result;

import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

/** Immediate, unconditional victory for an explicit {@link Competitor} */
public class CompetitorVictoryCondition extends ImmediateVictoryCondition {

  private final Competitor competitor;

  public CompetitorVictoryCondition(Competitor competitor) {
    this.competitor = assertNotNull(competitor);
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    return Boolean.compare(b == competitor, a == competitor);
  }

  @Override
  public Component getDescription(Match match) {
    return translatable(
        competitor.isNamePlural()
            ? "broadcast.gameOver.teamWinners"
            : "broadcast.gameOver.teamWinner",
        competitor.getName());
  }
}
