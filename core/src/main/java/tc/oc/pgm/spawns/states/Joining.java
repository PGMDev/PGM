package tc.oc.pgm.spawns.states;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.SpawnMatchModule;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;

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
