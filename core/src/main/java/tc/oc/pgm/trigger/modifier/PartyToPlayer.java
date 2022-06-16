package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;

public class PartyToPlayer implements Trigger.TParty {
  private final Trigger<? super MatchPlayer> child;

  public PartyToPlayer(Trigger<? super MatchPlayer> child) {
    this.child = child;
  }

  @Override
  public void trigger(Party party) {
    for (MatchPlayer player : party.getPlayers()) {
      child.trigger(player);
    }
  }
}
