package tc.oc.pgm.variables;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.filters.Filterable;

public class BlitzVariable implements Variable<MatchPlayer> {

  private final VariableDefinition<MatchPlayer> definition;

  public BlitzVariable(VariableDefinition<? extends Filterable<?>> definition) {
    this.definition = (VariableDefinition<MatchPlayer>) definition;
  }

  @Override
  public VariableDefinition<MatchPlayer> getDefinition() {
    return definition;
  }

  @Override
  public double getValue(Filterable<?> context) {
    MatchPlayer matchPlayer = context.getFilterableAncestor(MatchPlayer.class);
    BlitzMatchModule blitzMatchModule = matchPlayer.moduleRequire(BlitzMatchModule.class);
    return blitzMatchModule.getNumOfLives(matchPlayer.getId());
  }

  @Override
  public void setValue(Filterable<?> context, double value) {
    MatchPlayer matchPlayer = context.getFilterableAncestor(MatchPlayer.class);
    BlitzMatchModule blitzMatchModule = matchPlayer.moduleRequire(BlitzMatchModule.class);
    blitzMatchModule.setLives(matchPlayer, Math.max((int) value, 0));
  }
}
