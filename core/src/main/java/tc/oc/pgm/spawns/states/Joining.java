package tc.oc.pgm.spawns.states;

import static net.kyori.adventure.text.Component.empty;

import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinPartyEvent;
import tc.oc.pgm.spawns.SpawnMatchModule;

/** Player is waiting to spawn after joining a team */
public class Joining extends Spawning {

  private final boolean reset;

  public Joining(SpawnMatchModule smm, MatchPlayer player, long minSpawnTick, boolean reset) {
    super(smm, player, smm.getDeathTick(player), minSpawnTick);
    this.spawnRequested = true;
    this.reset = reset;
    this.permission = new StatePermissions.Observer();
  }

  @Override
  public void enterState() {
    player.setVisible(false);
    super.enterState();

    if (reset) Observing.resetPlayer(smm, player, true, false);
  }

  @Override
  protected Component getTitle(boolean spectator) {
    return empty();
  }

  @Override
  public void onEvent(PlayerJoinPartyEvent event) {
    super.onEvent(event);
    if (!(event.getNewParty() instanceof Competitor)) {
      transition(new Observing(smm, player, false, false));
    }
  }
}
