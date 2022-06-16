package tc.oc.pgm.trigger.triggers;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.trigger.Trigger;

public class KitTrigger implements Trigger.TPlayer {

  private final Kit kit;

  public KitTrigger(Kit kit) {
    this.kit = kit;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    matchPlayer.applyKit(kit, false);
  }
}
