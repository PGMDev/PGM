package tc.oc.pgm.action.replacements;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLFluentParser;

public class ReplacementParser {
  private static final NumberFormat DEFAULT_FORMAT = NumberFormat.getIntegerInstance();
  private final Map<String, Method> methodParsers =
      MethodParsers.getMethodParsersForClass(getClass());
  private final FeatureDefinitionContext features;
  private final XMLFluentParser parser;
  private final boolean isTopLevel;

  public ReplacementParser(MapFactory factory, boolean topLevel) {
    features = factory.getFeatures();
    parser = factory.getParser();
    isTopLevel = topLevel;
  }

  public ReplacementParser(MapFactory factory) {
    this(factory, false);
  }

  public <B extends Filterable<?>> Replacement parse(Element el, @Nullable Class<B> scope)
      throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (Replacement) parser.invoke(this, el, scope);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown replacement type: " + el.getName(), el);
    }
  }

  public Set<String> replacementTypes() {
    return methodParsers.keySet();
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  private <B extends Filterable<?>> Class<B> parseScope(Element el, Class<B> scope)
      throws InvalidXMLException {
    if (scope == null) return Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    Node node = Node.fromAttr(el, "scope");
    if (node != null && !Filterables.isAssignable(Filterables.parse(node), scope))
      throw new InvalidXMLException(
          "Wrong scope defined for replacement, scope must be " + scope.getSimpleName(), el);
    return scope;
  }

  @MethodParser("decimal")
  public <T extends Filterable<?>> Replacement parseDecimal(Element el, Class<T> scope)
      throws InvalidXMLException {
    scope = parseScope(el, scope);
    var formula = parser.formula(scope, el, "value").required();
    var format = parser
        .string(el, "format")
        .attr()
        .optional()
        .<NumberFormat>map(DecimalFormat::new)
        .orElse(DEFAULT_FORMAT);

    return ScopedReplacement.of(scope, ctx -> text(format.format(formula.applyAsDouble(ctx))));
  }

  @MethodParser("player")
  public <T extends Filterable<?>> Replacement parsePlayer(Element el, Class<T> scope)
      throws InvalidXMLException {
    var variable = parser.variable(el, "var").scope(MatchPlayer.class).singleExclusive();
    var fallback = parser.component(el, "fallback").optional(empty());
    var nameStyle = parser.parseEnum(NameStyle.class, el, "style").optional(NameStyle.VERBOSE);
    return filterable ->
        variable.getHolder(filterable).map(mp -> mp.getName(nameStyle)).orElse(fallback);
  }

  @MethodParser("switch")
  public <T extends Filterable<?>> Replacement parseSwitch(Element el, Class<T> scope)
      throws InvalidXMLException {
    scope = parseScope(el, scope);
    var formula = parser.formula(scope, el, "value").orNull();
    var fallback = parser.component(el, "fallback").child().optional(empty());
    var children = el.getChildren("case");
    var branches = new ArrayList<SwitchBranch>(children.size());

    for (var innerEl : children) {
      var valueRange = parser
          .doubleRange(innerEl, "match")
          .validate((r, n) -> {
            if (formula == null) {
              throw new InvalidXMLException(
                  "A match attribute is specified but there's no switch value to bind to", n);
            }
          })
          .optional();
      var filter = parser.filter(innerEl, "filter").respondsTo(scope).optional(() -> {
        if (valueRange.isEmpty())
          throw new InvalidXMLException(
              "At least a filter or a match attribute must be specified", innerEl);
        return StaticFilter.ALLOW;
      });
      var result = parser.component(innerEl, "result").required();
      branches.add(new SwitchBranch(result, valueRange.orElse(Range.all()), filter));
    }

    return ScopedReplacement.of(scope, ctx -> {
      var formulaResult = formula != null ? formula.applyAsDouble(ctx) : null;
      for (var branch : branches) {
        if ((formula == null || branch.valueRange.contains(formulaResult))
            && branch.filter.query(ctx).isAllowed()) return branch.result;
      }

      return fallback;
    });
  }

  @MethodParser("replacement")
  public <T extends Filterable<?>> Replacement parseReference(Element el, Class<T> scope)
      throws InvalidXMLException {
    if (isTopLevel)
      throw new InvalidXMLException(
          "References to replacements at the root level are not allowed", el);

    Replacement replacement = features.resolve(new Node(el), Replacement.class);

    if (!replacement.canUse(scope)) {
      throw new InvalidXMLException(
          replacement.getClass().getSimpleName() + " cannot be used with " + scope.getSimpleName(),
          el);
    }

    return replacement;
  }

  public record SwitchBranch(Component result, Range<Double> valueRange, Filter filter) {}
}
