package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;

public class WorldTimeVariable extends AbstractVariable<Match> {

  public static final WorldTimeVariable INSTANCE = new WorldTimeVariable();

  public WorldTimeVariable() {
    super(Match.class);
  }

  @Override
  protected double getValueImpl(Match match) {
    long val = match.getWorld().getTime();
    return (double) val;
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    long val = (long) value;
    match.getWorld().setTime(val);
  }
}
