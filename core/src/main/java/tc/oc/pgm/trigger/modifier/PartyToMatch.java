package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.trigger.Trigger;

public class PartyToMatch implements Trigger.TParty {
  private final Trigger<? super Match> child;

  public PartyToMatch(Trigger<? super Match> child) {
    this.child = child;
  }

  @Override
  public void trigger(Party party) {
    child.trigger(party.getMatch());
  }
}
