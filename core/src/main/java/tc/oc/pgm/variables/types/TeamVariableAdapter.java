package tc.oc.pgm.variables.types;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.variables.Variable;

public class TeamVariableAdapter extends AbstractVariable<Match>
    implements Variable.Indexed<Match>, StateHolder<Team> {

  private final Variable<Party> child;
  private final FeatureReference<TeamFactory> teamRef;

  public TeamVariableAdapter(Variable<Party> child, FeatureReference<TeamFactory> teamRef) {
    super(Match.class);
    this.child = child;
    this.teamRef = teamRef;
  }

  @Override
  public boolean isDynamic() {
    return child.isDynamic();
  }

  @Override
  public boolean isReadonly() {
    return child.isReadonly();
  }

  @Override
  public boolean isIndexed() {
    return child.isIndexed();
  }

  public void load(Match match) {
    match
        .getFeatureContext()
        .registerState(this, match.needModule(TeamMatchModule.class).getTeam(teamRef.get()));
  }

  @Override
  protected double getValueImpl(Match match) {
    return child.getValue(match.state(this));
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    child.setValue(match.state(this), value);
  }

  @Override
  public double getValue(Filterable<?> context, int idx) {
    return ((Variable.Indexed<Party>) child).getValue(context.state(this), idx);
  }

  @Override
  public void setValue(Filterable<?> context, int idx, double value) {
    ((Variable.Indexed<Party>) child).setValue(context.state(this), idx, value);
  }

  @Override
  public int size() {
    return ((Variable.Indexed<Party>) child).size();
  }
}
