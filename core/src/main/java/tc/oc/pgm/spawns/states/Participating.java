package tc.oc.pgm.spawns.states;

import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.spawns.SpawnMatchModule;

public class Participating extends State {

  public Participating(SpawnMatchModule smm, MatchPlayer player) {
    super(smm, player);
    this.permission = new StatePermissions.Participant();
  }

  @Override
  public void onEvent(MatchFinishEvent event) {
    super.onEvent(event);
    transition(new Observing(smm, player, true, false));
  }
}
