package tc.oc.pgm.result;

import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
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
    return translatable("victoryCondition.tie");
  }
}
