package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;

public class PickupFlagAction extends AbstractAction<MatchPlayer> {
  private final FeatureReference<? extends FlagDefinition> flag;

  public PickupFlagAction(FeatureReference<? extends FlagDefinition> flag) {
    super(MatchPlayer.class);
    this.flag = flag;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    final Flag flag = this.flag.get().getGoal(matchPlayer.getMatch());
    if (flag == null) return;
    flag.pickupFlag(matchPlayer, matchPlayer.getLocation());
  }
}
