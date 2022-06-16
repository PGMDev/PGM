package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;

public class PlayerToTeam implements Trigger.TPlayer {

  private final Trigger<? super Party> child;

  public PlayerToTeam(Trigger<? super Party> child) {
    this.child = child;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    child.trigger(matchPlayer.getParty());
  }
}
