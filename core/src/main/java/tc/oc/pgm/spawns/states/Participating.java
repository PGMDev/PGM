package tc.oc.pgm.spawns.states;

import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;

public class Participating extends State {

  public Participating(MatchPlayer player) {
    super(player);
    this.permission = new StatePermissions.Participant();
  }

  @Override
  public void onEvent(MatchFinishEvent event) {
    super.onEvent(event);
    transition(new Observing(player, true, false));
  }
}
