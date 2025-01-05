package tc.oc.pgm.variables;

import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.types.ArrayVariable;
import tc.oc.pgm.variables.types.CuboidVariable;
import tc.oc.pgm.variables.types.DummyVariable;
import tc.oc.pgm.variables.types.LivesVariable;
import tc.oc.pgm.variables.types.MaxBuildVariable;
import tc.oc.pgm.variables.types.PlayerLocationVariable;
import tc.oc.pgm.variables.types.ScoreVariable;
import tc.oc.pgm.variables.types.TeamVariableAdapter;
import tc.oc.pgm.variables.types.TimeLimitVariable;

public class VariableParser {
  // The limitation is due to them being used in exp4j formulas for.
  public static final Pattern VARIABLE_ID = Pattern.compile("[A-Za-z_][\\w.]*");

  private final MapFactory factory;
  private final Map<String, Method> methodParsers;

  public VariableParser(MapFactory factory) {
    this.factory = factory;
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  public Variable<?> parse(Element el) throws InvalidXMLException {
    String id = Node.fromRequiredAttr(el, "id").getValue();
    if (!VARIABLE_ID.matcher(id).matches() || id.contains("."))
      throw new InvalidXMLException(
          "Variable IDs must start with a letter or underscore and can only include letters, digits or underscores.",
          el);

    Method parser = methodParsers.get(el.getName().toLowerCase());
    if (parser != null) {
      try {
        return (Variable<?>) parser.invoke(this, el);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown variable type: " + el.getName(), el);
    }
  }

  @MethodParser("variable")
  public Variable<?> parseDummy(Element el) throws InvalidXMLException {
    Class<? extends Filterable<?>> scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    double def = XMLUtils.parseNumber(Node.fromAttr(el, "default"), Double.class, 0d);
    Integer excl = XMLUtils.parseNumberInRange(
        Node.fromAttr(el, "exclusive"), Integer.class, Range.closed(1, 50), null);
    return new DummyVariable<>(scope, def, excl);
  }

  @MethodParser("array")
  public Variable<?> parseArray(Element el) throws InvalidXMLException {
    Class<? extends Filterable<?>> scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    int size = XMLUtils.parseNumberInRange(
        Node.fromRequiredAttr(el, "size"), Integer.class, Range.closed(1, 1024));
    double def = XMLUtils.parseNumber(Node.fromAttr(el, "default"), Double.class, 0d);
    return new ArrayVariable<>(scope, size, def);
  }

  @MethodParser("lives")
  public Variable<MatchPlayer> parseBlitzLives(Element el) {
    return LivesVariable.INSTANCE;
  }

  @MethodParser("score")
  public Variable<Party> parseScore(Element el) {
    return ScoreVariable.INSTANCE;
  }

  @MethodParser("timelimit")
  public Variable<Match> parseTimeLimit(Element el) {
    return TimeLimitVariable.INSTANCE;
  }

  @MethodParser("with-team")
  public Variable<Match> parseTeamAdapter(Element el) throws InvalidXMLException {
    var features = factory.getFeatures();
    @SuppressWarnings("unchecked")
    Variable<Party> var = features.resolve(Node.fromRequiredAttr(el, "var"), Variable.class);
    if (var.getScope() != Party.class) {
      throw new InvalidXMLException(
          "Team scope is required for with-team variable, got " + var.getScope().getSimpleName(),
          el);
    }

    var team = features.createReference(Node.fromRequiredAttr(el, "team"), TeamFactory.class);

    return new TeamVariableAdapter(var, team);
  }

  @MethodParser("maxbuildheight")
  public Variable<Match> parseMaxBuild(Element el) {
    return MaxBuildVariable.INSTANCE;
  }

  @MethodParser("player-location")
  public Variable<MatchPlayer> parsePlayerLocation(Element el) throws InvalidXMLException {
    var component =
        XMLUtils.parseEnum(Node.fromAttr(el, "component"), PlayerLocationVariable.Component.class);
    return PlayerLocationVariable.INSTANCES.get(component);
  }

  @MethodParser("cuboid")
  public Variable<Match> parseCuboid(Element el) throws InvalidXMLException {
    String baseId = FeatureDefinitionContext.parseId(el);
    var variable = new CuboidVariable(factory.getRegions().parseCuboid(el));
    for (CuboidVariable.Component component : CuboidVariable.Component.values()) {
      var subId = baseId + "." + component.name().toLowerCase(Locale.ROOT);
      factory.getFeatures().addFeature(el, subId, variable.getComponent(component));
    }
    return variable;
  }
}
