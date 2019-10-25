package tc.oc.pgm.result;

import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.Match;

/** Everybody loses */
public class TieVictoryCondition extends ImmediateVictoryCondition {

  @Override
  public int compare(Competitor a, Competitor b) {
    return 0;
  }

  @Override
  public Component getDescription(Match match) {
    return new PersonalizedText("nobody wins");
  }
}
