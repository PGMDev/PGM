package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;

public class PlayerToMatch implements Trigger.TPlayer {

  private final Trigger<? super Match> child;

  public PlayerToMatch(Trigger<? super Match> child) {
    this.child = child;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    child.trigger(matchPlayer.getMatch());
  }
}
