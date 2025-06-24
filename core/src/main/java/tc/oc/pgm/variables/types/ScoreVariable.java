package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.score.ScoreCause;
import tc.oc.pgm.score.ScoreMatchModule;

public class ScoreVariable extends AbstractVariable<Party> {
  public static final ScoreVariable INSTANCE = new ScoreVariable();

  public ScoreVariable() {
    super(Party.class);
  }

  @Override
  protected double getValueImpl(Party party) {
    if (party instanceof Competitor c)
      return party
          .moduleOptional(ScoreMatchModule.class)
          .map(smm -> smm.getScore(c))
          .orElse(-1d);
    return -1;
  }

  @Override
  protected void setValueImpl(Party party, double value) {
    if (party instanceof Competitor c)
      party
          .moduleOptional(ScoreMatchModule.class)
          .ifPresent(smm -> smm.setScore(c, value, ScoreCause.VARIABLE));
  }
}
