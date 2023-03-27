package tc.oc.pgm.action;

import static net.kyori.adventure.text.Component.empty;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Method;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.actions.ActionNode;
import tc.oc.pgm.action.actions.ExposedAction;
import tc.oc.pgm.action.actions.FillAction;
import tc.oc.pgm.action.actions.KillEntitiesAction;
import tc.oc.pgm.action.actions.MessageAction;
import tc.oc.pgm.action.actions.ReplaceItemAction;
import tc.oc.pgm.action.actions.ScopeSwitchAction;
import tc.oc.pgm.action.actions.SetVariableAction;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.VariableDefinition;
import tc.oc.pgm.variables.VariablesModule;

public class ActionParser {

  private final MapFactory factory;
  private final FeatureDefinitionContext features;
  private final FilterParser filters;
  private final RegionParser regions;
  private final VariablesModule variables;
  private final Map<String, Method> methodParsers;

  public ActionParser(MapFactory factory) {
    this.factory = factory;
    this.features = factory.getFeatures();
    this.filters = factory.getFilters();
    this.regions = factory.getRegions();
    this.variables = factory.needModule(VariablesModule.class);
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  @SuppressWarnings("unchecked")
  public <B extends Filterable<?>> Action<? super B> parse(Element el, @Nullable Class<B> bound)
      throws InvalidXMLException {
    String id = FeatureDefinitionContext.parseId(el);

    Node node = new Node(el);
    if (id != null && maybeReference(el)) {
      return parseReference(node, id, bound);
    }

    Action<? super B> result = parseDynamic(el, bound);
    if (bound != null) validate(result, ActionScopeValidation.of(bound), node);
    if (result instanceof ActionDefinition) {
      if (XMLUtils.parseBoolean(Node.fromAttr(el, "expose"), false)) {

        if (id == null)
          throw new InvalidXMLException("Attribute 'id' is required for exposed actions", el);

        if (!result.getScope().isAssignableFrom(Match.class))
          throw new InvalidXMLException("Match scope is required for exposed actions", el);

        result =
            (ActionDefinition<? super B>)
                new ExposedAction(id, (ActionDefinition<? super Match>) result);
      }

      features.addFeature(el, (ActionDefinition<? super B>) result);
    }
    return result;
  }

  public boolean isAction(Element el) {
    return getParserFor(el) != null;
  }

  private boolean maybeReference(Element el) {
    return "action".equals(el.getName()) && el.getChildren().isEmpty();
  }

  public <B> Action<? super B> parseReference(Node node, Class<B> bound)
      throws InvalidXMLException {
    return parseReference(node, node.getValue(), bound);
  }

  public <B> Action<? super B> parseReference(Node node, String id, Class<B> bound)
      throws InvalidXMLException {
    Action<? super B> action = features.addReference(new XMLActionReference<>(features, node, id));
    validate(action, ActionScopeValidation.of(bound), node);
    return action;
  }

  public <B extends Filterable<?>> Action<? super B> parseProperty(
      @NotNull Node node, Class<B> bound) throws InvalidXMLException {
    if (node.isAttribute()) return this.parseReference(node, bound);

    ActionNode<? super B> result = this.parseAction(node.getElement(), bound);

    if (bound != null) validate(result, ActionScopeValidation.of(bound), node);
    features.addFeature(node.getElement(), result);
    return result;
  }

  @SuppressWarnings("rawtypes, unchecked")
  public void validate(
      Action<?> action, FeatureValidation<ActionDefinition<?>> validation, Node node)
      throws InvalidXMLException {
    if (action instanceof XMLFeatureReference) {
      factory.getFeatures().validate((XMLFeatureReference) action, validation);
    } else if (action instanceof ActionDefinition) {
      factory.getFeatures().validate((ActionDefinition) action, validation, node);
    } else {
      throw new IllegalStateException(
          "Attempted validation on an action which is neither definition nor reference.");
    }
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  @SuppressWarnings("unchecked")
  private <T, B extends Filterable<?>> Action<T> parseDynamic(Element el, Class<B> scope)
      throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (Action<T>) parser.invoke(this, el, scope);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown action type: " + el.getName(), el);
    }
  }

  public <T extends Filterable<?>> Trigger<T> parseTrigger(Element el) throws InvalidXMLException {
    Class<T> cls = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    return new Trigger<>(
        cls,
        filters.parseRequiredProperty(el, "filter", DynamicFilterValidation.of(cls)),
        parseProperty(Node.fromRequiredChildOrAttr(el, "action", "trigger"), cls));
  }

  private <B extends Filterable<?>> Class<B> parseScope(Element el, Class<B> scope)
      throws InvalidXMLException {
    return parseScope(el, scope, "scope");
  }

  private <B extends Filterable<?>> Class<B> parseScope(Element el, Class<B> scope, String attr)
      throws InvalidXMLException {
    if (scope == null) return Filterables.parse(Node.fromRequiredAttr(el, attr));

    Node node = Node.fromAttr(el, attr);
    if (node != null && Filterables.parse(node) != scope)
      throw new InvalidXMLException(
          "Wrong scope defined for action, scope must be " + scope.getSimpleName(), el);
    return scope;
  }

  @MethodParser("action")
  public <B extends Filterable<?>> ActionNode<? super B> parseAction(Element el, Class<B> scope)
      throws InvalidXMLException {
    scope = parseScope(el, scope);

    ImmutableList.Builder<Action<? super B>> builder = ImmutableList.builder();
    for (Element child : el.getChildren()) {
      builder.add(parse(child, scope));
    }

    Filter filter = filters.parseFilterProperty(el, "filter", StaticFilter.ALLOW);
    Filter untriggerFilter = filters.parseFilterProperty(el, "untrigger-filter", StaticFilter.DENY);

    return new ActionNode<>(builder.build(), filter, untriggerFilter, scope);
  }

  @MethodParser("switch-scope")
  public <O extends Filterable<?>, I extends Filterable<?>> Action<? super O> parseSwitchScope(
      Element el, Class<O> outer) throws InvalidXMLException {
    outer = parseScope(el, outer, "outer");
    Class<I> inner = parseScope(el, null, "inner");

    ActionDefinition<? super I> child = parseAction(el, inner);

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
  public MessageAction parseChatMessage(Element el, Class<?> scope) throws InvalidXMLException {
    Component text = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "text"));
    Component actionbar = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "actionbar"));

    Node titleNode = Node.fromChildOrAttr(el, "title");
    Node subtitleNode = Node.fromChildOrAttr(el, "subtitle");
    Title title = null;
    if (titleNode != null || subtitleNode != null)
      title =
          Title.title(
              XMLUtils.parseFormattedText(titleNode, empty()),
              XMLUtils.parseFormattedText(subtitleNode, empty()),
              XMLUtils.parseTitleTimes(el, Title.DEFAULT_TIMES));

    if (text == null && actionbar == null && title == null)
      throw new InvalidXMLException(
          "Expected at least one of text, title, subtitle or actionbar", el);

    return new MessageAction(text, actionbar, title);
  }

  @MethodParser("set")
  public <T extends Filterable<?>> SetVariableAction<T> parseSetVariable(Element el, Class<T> scope)
      throws InvalidXMLException {
    VariableDefinition<?> var =
        features.resolve(Node.fromRequiredAttr(el, "var"), VariableDefinition.class);
    scope = parseScope(el, scope);

    if (!Filterables.isAssignable(scope, var.getScope()))
      throw new InvalidXMLException(
          "Wrong variable scope for '"
              + var.getId()
              + "', expected "
              + var.getScope().getSimpleName()
              + " which cannot be found in "
              + scope.getSimpleName(),
          el);

    String expression = Node.fromRequiredAttr(el, "value").getValue();
    Formula<T> formula =
        Formula.of(
            expression, variables.getVariableNames(scope), variables.getContextBuilder(scope));

    return new SetVariableAction<>(scope, var, formula);
  }

  @MethodParser("kill-entities")
  public KillEntitiesAction parseKillEntities(Element el, Class<?> scope)
      throws InvalidXMLException {
    return new KillEntitiesAction(filters.parseProperty(el, "filter"));
  }

  @MethodParser("replace-item")
  public ReplaceItemAction parseReplaceItem(Element el, Class<?> scope) throws InvalidXMLException {
    ItemMatcher matcher = factory.getKits().parseItemMatcher(el, "find");
    ItemStack item = factory.getKits().parseItem(el.getChild("replace"), true);

    boolean keepAmount = XMLUtils.parseBoolean(el.getAttribute("keep-amount"), false);
    boolean keepEnchants = XMLUtils.parseBoolean(el.getAttribute("keep-enchants"), false);

    return new ReplaceItemAction(matcher, item, keepAmount, keepEnchants);
  }

  @MethodParser("fill")
  public FillAction parseFill(Element el, Class<?> scope) throws InvalidXMLException {
    return new FillAction(
        regions.parseProperty(Node.fromAttrOrSelf(el, "region"), BlockBoundedValidation.INSTANCE),
        XMLUtils.parseMaterialData(Node.fromRequiredAttr(el, "material")),
        filters.parseProperty(Node.fromAttr(el, "filter")),
        XMLUtils.parseBoolean(el.getAttribute("events"), false));
  }
}
