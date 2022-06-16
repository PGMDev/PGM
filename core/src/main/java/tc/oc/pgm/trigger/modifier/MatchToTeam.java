package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.trigger.Trigger;

public class MatchToTeam implements Trigger.TMatch {
  private final Trigger<? super Party> child;

  public MatchToTeam(Trigger<? super Party> child) {
    this.child = child;
  }

  @Override
  public void trigger(Match match) {
    for (Party party : match.getParties()) {
      child.trigger(party);
    }
  }
}
