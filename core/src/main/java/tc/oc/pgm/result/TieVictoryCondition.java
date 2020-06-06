package tc.oc.pgm.result;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

/** Everybody loses */
public class TieVictoryCondition extends ImmediateVictoryCondition {

  @Override
  public int compare(Competitor a, Competitor b) {
    return 0;
  }

  @Override
  public Component getDescription(Match match) {
    return TranslatableComponent.of("victoryCondition.tie");
  }
}
