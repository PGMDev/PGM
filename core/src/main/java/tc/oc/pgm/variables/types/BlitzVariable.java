package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.variables.VariableDefinition;

public class BlitzVariable extends AbstractVariable<MatchPlayer, VariableDefinition<MatchPlayer>> {

  private BlitzMatchModule bmm;

  public BlitzVariable(VariableDefinition<?> definition) {
    super((VariableDefinition<MatchPlayer>) definition);
  }

  @Override
  public void postLoad(Match match) {
    bmm = match.moduleRequire(BlitzMatchModule.class);
  }

  @Override
  protected double getValueImpl(MatchPlayer player) {
    return bmm.getNumOfLives(player.getId());
  }

  @Override
  protected void setValueImpl(MatchPlayer player, double value) {
    bmm.setLives(player, Math.max((int) value, 0));
  }
}
