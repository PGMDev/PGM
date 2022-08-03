package tc.oc.pgm.action;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.util.Map;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.actions.ActionNode;
import tc.oc.pgm.action.actions.ChatMessageAction;
import tc.oc.pgm.action.actions.ScopeSwitchAction;
import tc.oc.pgm.action.actions.SetVariableAction;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.VariableDefinition;
import tc.oc.pgm.variables.VariablesModule;

public class ActionParser {

  private final MapFactory factory;
  private final FilterParser filters;
  private final Map<String, Method> methodParsers;

  public ActionParser(MapFactory factory) {
    this.factory = factory;
    this.filters = factory.getFilters();
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  public <B extends Filterable<?>> Action<? super B> parse(Element el, @Nullable Class<B> bound)
      throws InvalidXMLException {
    String id = FeatureDefinitionContext.parseId(el);

    if (id != null && maybeReference(el)) {
      return parseReference(new Node(el), id, bound);
    }

    Action<? super B> result = parseDynamic(el, bound);

    Class<?> childBound = result.getScope();
    if (bound != null && !childBound.isAssignableFrom(bound)) {
      throw new InvalidXMLException(
          "Wrong trigger target, expected "
              + bound.getSimpleName()
              + " rather than "
              + childBound.getSimpleName(),
          new Node(el));
    }

    // We don't need to add references, they should already be added by whoever created them.
    if (result instanceof ActionDefinition)
      //noinspection unchecked
      factory.getFeatures().addFeature(el, (ActionDefinition<? super B>) result);
    return result;
  }

  public boolean isAction(Element el) {
    return getParserFor(el) != null;
  }

  private boolean maybeReference(Element el) {
    return "trigger".equals(el.getName()) && el.getChildren().isEmpty();
  }

  public <B> Action<? super B> parseReference(Node node, Class<B> bound)
      throws InvalidXMLException {
    return parseReference(node, node.getValue(), bound);
  }

  public <B> Action<? super B> parseReference(Node node, String id, Class<B> bound)
      throws InvalidXMLException {
    return factory
        .getFeatures()
        .addReference(new XMLActionReference<>(factory.getFeatures(), node, id, bound));
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  @SuppressWarnings("unchecked")
  private <T, B extends Filterable<?>> Action<T> parseDynamic(Element el, Class<B> bound)
      throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (Action<T>) parser.invoke(this, el, bound);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown trigger type: " + el.getName(), el);
    }
  }

  // Warning: this should only be used when you're certain those features load before filters.
  private <T extends FeatureDefinition> T resolve(Node node, Class<T> cls)
      throws InvalidXMLException {
    XMLFeatureReference<T> ref = this.factory.getFeatures().createReference(node, cls);
    ref.resolve();
    return ref.get();
  }

  public <T extends Filterable<?>> Trigger<T> parseTrigger(Element el) throws InvalidXMLException {
    Class<T> cls = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    return new Trigger<>(
        cls,
        filters.parseReference(Node.fromRequiredAttr(el, "filter")),
        parseReference(Node.fromRequiredAttr(el, "trigger"), cls));
  }

  @MethodParser("action")
  public <B extends Filterable<?>> ActionDefinition<? super B> parseDefinition(
      Element el, Class<B> bound) throws InvalidXMLException {
    if (bound == null) bound = Filterables.parse(Node.fromRequiredAttr(el, "scope"));

    ImmutableList.Builder<Action<? super B>> builder = ImmutableList.builder();
    for (Element child : el.getChildren()) {
      builder.add(parse(child, bound));
    }

    return new ActionNode<>(builder.build(), bound);
  }

  @MethodParser("switch-scope")
  public <O extends Filterable<?>, I extends Filterable<?>> Action<? super O> parseSwitchScope(
      Element el, Class<O> outer) throws InvalidXMLException {
    if (outer == null) outer = Filterables.parse(Node.fromRequiredAttr(el, "outer"));
    Class<I> inner = Filterables.parse(Node.fromRequiredAttr(el, "inner"));

    ActionDefinition<? super I> child = parseDefinition(el, inner);

    Action<? super O> result = ScopeSwitchAction.of(child, outer, inner);
    if (result == null) {
      throw new InvalidXMLException(
          "Could not convert from " + outer.getSimpleName() + " to " + inner.getSimpleName(), el);
    }
    return result;
  }

  @MethodParser("kit")
  public Kit parseKitTrigger(Element el, Class<?> scope) throws InvalidXMLException {
    return factory.getKits().parse(el);
  }

  @MethodParser("message")
  public ChatMessageAction parseChatMessage(Element el, Class<?> scope) throws InvalidXMLException {
    return new ChatMessageAction(XMLUtils.parseFormattedText(Node.fromRequiredAttr(el, "text")));
  }

  @MethodParser("set")
  public <T extends Filterable<?>> SetVariableAction<T> parseSetVariable(Element el, Class<T> scope)
      throws InvalidXMLException {
    VariableDefinition<?> var =
        this.factory
            .getFeatures()
            .get(Node.fromRequiredAttr(el, "var").getValue(), VariableDefinition.class);
    if (scope == null) scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));

    if (!Filterables.isAssignable(scope, var.getScope()))
      throw new InvalidXMLException(
          "Wrong variable scope for '"
              + var.getId()
              + "', expected "
              + var.getScope().getSimpleName()
              + " which cannot be found in "
              + scope.getSimpleName(),
          el);

    VariablesModule vm = this.factory.needModule(VariablesModule.class);

    String expression = Node.fromRequiredAttr(el, "formula").getValue();
    Formula<T> formula =
        Formula.of(expression, vm.getVariableNames(scope), vm.getContextBuilder(scope));

    return new SetVariableAction<>(scope, var, formula);
  }
}
