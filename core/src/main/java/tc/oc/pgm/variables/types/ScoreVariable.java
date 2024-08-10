package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.score.ScoreMatchModule;

public class ScoreVariable extends AbstractVariable<Party> {
  public static final ScoreVariable INSTANCE = new ScoreVariable();

  public ScoreVariable() {
    super(Party.class);
  }

  @Override
  protected double getValueImpl(Party party) {
    if (party instanceof Competitor)
      return party.moduleRequire(ScoreMatchModule.class).getScore((Competitor) party);
    return 0;
  }

  @Override
  protected void setValueImpl(Party party, double value) {
    if (party instanceof Competitor)
      party.moduleRequire(ScoreMatchModule.class).setScore((Competitor) party, value);
  }
}
