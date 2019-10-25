package tc.oc.pgm.spawns.states;

import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.spawns.SpawnMatchModule;

/** Player is waiting to spawn after joining a team */
public class Joining extends Spawning {

  public Joining(SpawnMatchModule smm, MatchPlayer player) {
    super(smm, player);
    this.spawnRequested = true;
  }

  @Override
  public void enterState() {
    player.setVisible(false);

    super.enterState();
  }

  @Override
  protected Component getTitle() {
    return new PersonalizedText();
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    super.onEvent(event);
    if (!(event.getNewParty() instanceof Competitor)) {
      transition(new Observing(smm, player, false, false));
    }
  }
}
