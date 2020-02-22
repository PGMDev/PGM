package tc.oc.pgm.result;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;

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
