package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.regions.RegionMatchModule;

public class MaxBuildVariable extends AbstractVariable<Match> {

  public static final MaxBuildVariable INSTANCE = new MaxBuildVariable();

  public MaxBuildVariable() {
    super(Match.class);
  }

  @Override
  protected double getValueImpl(Match match) {
    Integer val = match.moduleRequire(RegionMatchModule.class).getMaxBuildHeight();
    return val == null ? -1 : val;
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    int val = (int) value;
    match.moduleRequire(RegionMatchModule.class).setMaxBuildHeight(val <= -1 ? null : val);
  }
}
