package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariableDefinition;

public class TeamVariableAdapter extends AbstractVariable<Match> implements IndexedVariable<Match> {

  private final VariableDefinition<Party> childRef;
  private final FeatureReference<TeamFactory> teamRef;

  private Variable<Party> child;
  private Team team;

  public TeamVariableAdapter(
      VariableDefinition<Match> definition,
      VariableDefinition<Party> childRef,
      FeatureReference<TeamFactory> teamRef) {
    super(definition);
    this.childRef = childRef;
    this.teamRef = teamRef;
  }

  public void postLoad(Match match) {
    team = match.needModule(TeamMatchModule.class).getTeam(teamRef.get());
    child = childRef.getVariable(match);
  }

  @Override
  protected double getValueImpl(Match match) {
    return child.getValue(team);
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    child.setValue(team, value);
  }

  @Override
  public double getValue(Filterable<?> context, int idx) {
    return ((IndexedVariable<Party>) child).getValue(team, idx);
  }

  @Override
  public void setValue(Filterable<?> context, int idx, double value) {
    ((IndexedVariable<Party>) child).setValue(team, idx, value);
  }

  @Override
  public int size() {
    return ((IndexedVariable<Party>) child).size();
  }
}
