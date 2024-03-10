package tc.oc.pgm.variables;

import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.types.ArrayVariable;
import tc.oc.pgm.variables.types.BlitzVariable;
import tc.oc.pgm.variables.types.DummyVariable;
import tc.oc.pgm.variables.types.MaxBuildVariable;
import tc.oc.pgm.variables.types.ScoreVariable;
import tc.oc.pgm.variables.types.TeamVariableAdapter;
import tc.oc.pgm.variables.types.TimeLimitVariable;

public class VariableParser {
  // The limitation is due to them being used in exp4j formulas for.
  public static final Pattern VARIABLE_ID = Pattern.compile("[A-Za-z_]\\w*");

  private final MapFactory factory;
  private final Map<String, Method> methodParsers;

  public VariableParser(MapFactory factory) {
    this.factory = factory;
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  public VariableDefinition<?> parse(Element el) throws InvalidXMLException {
    String id = Node.fromRequiredAttr(el, "id").getValue();
    if (!VARIABLE_ID.matcher(id).matches())
      throw new InvalidXMLException(
          "Variable IDs must start with a letter or underscore and can only include letters, digits or underscores.",
          el);

    Method parser = methodParsers.get(el.getName().toLowerCase());
    if (parser != null) {
      try {
        return (VariableDefinition<?>) parser.invoke(this, el, id);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown variable type: " + el.getName(), el);
    }
  }

  @MethodParser("variable")
  public VariableDefinition<?> parseDummy(Element el, String id) throws InvalidXMLException {
    Class<? extends Filterable<?>> scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    double def = XMLUtils.parseNumber(Node.fromAttr(el, "default"), Double.class, 0d);
    Integer excl =
        XMLUtils.parseNumberInRange(
            Node.fromAttr(el, "exclusive"), Integer.class, Range.closed(1, 50), null);
    return new VariableDefinition<>(
        id, scope, true, false, vd -> new DummyVariable<>(vd, def, excl));
  }

  @MethodParser("array")
  public VariableDefinition<?> parseArray(Element el, String id) throws InvalidXMLException {
    Class<? extends Filterable<?>> scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    int size =
        XMLUtils.parseNumberInRange(
            Node.fromRequiredAttr(el, "size"), Integer.class, Range.closed(1, 1024));
    double def = XMLUtils.parseNumber(Node.fromAttr(el, "default"), Double.class, 0d);
    return new VariableDefinition<>(
        id, scope, true, true, vd -> new ArrayVariable<>(vd, size, def));
  }

  @MethodParser("lives")
  public VariableDefinition<MatchPlayer> parseBlitzLives(Element el, String id)
      throws InvalidXMLException {
    return VariableDefinition.ofStatic(id, MatchPlayer.class, BlitzVariable::new);
  }

  @MethodParser("score")
  public VariableDefinition<Party> parseScore(Element el, String id) throws InvalidXMLException {
    return VariableDefinition.ofStatic(id, Party.class, ScoreVariable::new);
  }

  @MethodParser("timelimit")
  public VariableDefinition<Match> parseTimeLimit(Element el, String id)
      throws InvalidXMLException {
    return VariableDefinition.ofStatic(id, Match.class, TimeLimitVariable::new);
  }

  @MethodParser("with-team")
  public VariableDefinition<Match> parseTeamAdapter(Element el, String id)
      throws InvalidXMLException {
    @SuppressWarnings("unchecked")
    VariableDefinition<Party> var =
        factory.getFeatures().resolve(Node.fromRequiredAttr(el, "var"), VariableDefinition.class);
    if (var.getScope() != Party.class) {
      throw new InvalidXMLException(
          "Team scope is required for with-team variable, got " + var.getScope().getSimpleName(),
          el);
    }

    FeatureReference<TeamFactory> team =
        factory.getFeatures().createReference(Node.fromRequiredAttr(el, "team"), TeamFactory.class);

    return new VariableDefinition<>(
        id,
        Match.class,
        var.isDynamic(),
        var.isIndexed(),
        vd -> new TeamVariableAdapter(vd, var, team));
  }

  @MethodParser("maxbuildheight")
  public VariableDefinition<Match> parseMaxBuild(Element el, String id) throws InvalidXMLException {
    return VariableDefinition.ofStatic(id, Match.class, MaxBuildVariable::new);
  }
}
