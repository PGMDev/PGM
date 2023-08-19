package tc.oc.pgm.variables.types;

import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariableDefinition;

public class TeamVariable
    extends AbstractVariable<Match, VariableDefinition.Context<Match, TeamVariable.Context>> {

  private Variable<Party> child;
  private Team team;

  public TeamVariable(VariableDefinition<?> definition) {
    super((VariableDefinition.Context<Match, Context>) definition);
  }

  public void postLoad(Match match) {
    team = match.needModule(TeamMatchModule.class).getTeam(definition.getContext().teamRef.get());
    child = definition.getContext().childRef.getVariable(match);
  }

  @Override
  protected double getValueImpl(Match match) {
    return child.getValue(team);
  }

  @Override
  protected void setValueImpl(Match match, double value) {
    child.setValue(team, value);
  }

  public static class Context {
    private final VariableDefinition<Party> childRef;
    private final FeatureReference<TeamFactory> teamRef;

    public Context(MapFactory factory, Element el) throws InvalidXMLException {
      VariableDefinition<?> var =
          factory.getFeatures().resolve(Node.fromRequiredAttr(el, "var"), VariableDefinition.class);
      if (var.getScope() != Party.class) {
        throw new InvalidXMLException(
            "Wrong scope defined for var, scope must be " + var.getScope().getSimpleName(), el);
      }
      this.childRef = (VariableDefinition<Party>) var;
      this.teamRef =
          factory
              .getFeatures()
              .createReference(Node.fromRequiredAttr(el, "team"), TeamFactory.class);
    }
  }
}
