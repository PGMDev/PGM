package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.variables.VariableDefinition;

public class ScoreVariable extends AbstractVariable<Party, VariableDefinition<Party>> {

  private ScoreMatchModule smm;

  public ScoreVariable(VariableDefinition<?> definition) {
    super((VariableDefinition<Party>) definition);
  }

  @Override
  public void postLoad(Match match) {
    smm = match.moduleRequire(ScoreMatchModule.class);
  }

  @Override
  protected double getValueImpl(Party party) {
    if (party instanceof Competitor) return smm.getScore((Competitor) party);
    return 0;
  }

  @Override
  protected void setValueImpl(Party party, double value) {
    if (party instanceof Competitor) smm.setScore((Competitor) party, value);
  }
}
