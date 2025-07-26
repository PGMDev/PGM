package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.FlagDefinition;

public class DropFlagAction extends AbstractAction<Match> {
  private final FeatureReference<? extends FlagDefinition> flag;

  public DropFlagAction(FeatureReference<? extends FlagDefinition> flag) {
    super(Match.class);
    this.flag = flag;
  }

  @Override
  public void trigger(Match match) {
    final Flag flag = this.flag.get().getGoal(match);
    if (flag == null) return;
    flag.dropFlag();
  }
}
