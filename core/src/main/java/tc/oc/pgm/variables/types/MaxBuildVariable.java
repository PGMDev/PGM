package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.RegionMatchModule;
import tc.oc.pgm.variables.VariableDefinition;

public class MaxBuildVariable extends AbstractVariable<Match> {

  private RegionMatchModule rmm;

  public MaxBuildVariable(VariableDefinition<Match> definition) {
    super(definition);
  }

  @Override
  public void postLoad(Match match) {
    rmm = match.moduleRequire(RegionMatchModule.class);
  }

  @Override
  protected double getValueImpl(Match player) {
    Integer val = rmm.getMaxBuildHeight();
    return val == null ? -1 : val;
  }

  @Override
  protected void setValueImpl(Match player, double value) {
    int val = (int) value;
    rmm.setMaxBuildHeight(val <= -1 ? null : val);
  }
}
