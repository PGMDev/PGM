package tc.oc.pgm.trigger.modifier;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.trigger.Trigger;

public class MatchToPlayer implements Trigger.TMatch {
  private final Trigger<? super MatchPlayer> child;

  public MatchToPlayer(Trigger<? super MatchPlayer> child) {
    this.child = child;
  }

  @Override
  public void trigger(Match match) {
    for (MatchPlayer player : match.getParticipants()) {
      child.trigger(player);
    }
  }
}
