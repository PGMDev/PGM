package tc.oc.pgm.action;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.sound.Sound;
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
import tc.oc.pgm.action.actions.PasteStructureAction;
import tc.oc.pgm.action.actions.ReplaceItemAction;
import tc.oc.pgm.action.actions.ScopeSwitchAction;
import tc.oc.pgm.action.actions.SetVariableAction;
import tc.oc.pgm.action.actions.SoundAction;
import tc.oc.pgm.action.actions.TakePaymentAction;
import tc.oc.pgm.action.actions.TeleportAction;
import tc.oc.pgm.action.actions.VelocityAction;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.player.ParticipatingFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.shops.ShopModule;
import tc.oc.pgm.shops.menu.Payable;
import tc.oc.pgm.structure.StructureDefinition;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesModule;

public class ActionParser {

  private static final NumberFormat DEFAULT_FORMAT = NumberFormat.getIntegerInstance();

  private final MapFactory factory;
  private final boolean legacy;
  private final FeatureDefinitionContext features;
  private final FilterParser filters;
  private final RegionParser regions;
  private final VariablesModule variables;
  private final Map<String, Method> methodParsers;

  public ActionParser(MapFactory factory) {
    this.factory = factory;
    this.legacy = !factory.getProto().isNoOlderThan(MapProtos.ACTION_REVAMP);
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

        result = new ExposedAction(id, (ActionDefinition<Filterable<?>>) result);
      }

      features.addFeature(el, (ActionDefinition<? super B>) result);
    }
    return result;
  }

  public Set<String> actionTypes() {
    return methodParsers.keySet();
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
      @Nullable Node node, Class<B> bound, Action<? super B> def) throws InvalidXMLException {
    return node == null ? def : parseProperty(node, bound);
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

  private <B extends Filterable<?>> boolean includeObs(Element el, Class<B> scope)
      throws InvalidXMLException {
    return !PartyQuery.class.isAssignableFrom(scope)
        || XMLUtils.parseBoolean(el.getAttribute("observers"), legacy);
  }

  private Filter wrapFilter(Filter outer, boolean includeObs) {
    if (includeObs || outer == StaticFilter.DENY) return outer;
    if (outer == StaticFilter.ALLOW) return ParticipatingFilter.PARTICIPATING;
    return AllFilter.of(outer, ParticipatingFilter.PARTICIPATING);
  }

  // Parser for <trigger> elements
  public <T extends Filterable<?>> Trigger<T> parseTrigger(Element el) throws InvalidXMLException {
    Class<T> cls = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
    return new Trigger<>(
        cls,
        wrapFilter(
            filters.parseRequiredProperty(el, "filter", DynamicFilterValidation.of(cls)),
            includeObs(el, cls)),
        parseProperty(Node.fromRequiredChildOrAttr(el, "action", "trigger"), cls));
  }

  // Generic action with N children parser
  public <B extends Filterable<?>> ActionNode<? super B> parseAction(
      Element el, Class<B> scope, boolean includeObs) throws InvalidXMLException {
    scope = parseScope(el, scope);

    ImmutableList.Builder<Action<? super B>> builder = ImmutableList.builder();
    for (Element child : el.getChildren()) {
      builder.add(parse(child, scope));
    }

    Filter filter =
        wrapFilter(filters.parseFilterProperty(el, "filter", StaticFilter.ALLOW), includeObs);
    Filter untriggerFilter = wrapFilter(
        filters.parseFilterProperty(
            el, "untrigger-filter", legacy ? StaticFilter.DENY : StaticFilter.ALLOW),
        includeObs);

    return new ActionNode<>(builder.build(), filter, untriggerFilter, scope);
  }

  // Parsers
  @MethodParser("action")
  public <B extends Filterable<?>> ActionNode<? super B> parseAction(Element el, Class<B> scope)
      throws InvalidXMLException {
    return parseAction(el, scope, true);
  }

  @MethodParser("switch-scope")
  public <O extends Filterable<?>, I extends Filterable<?>> Action<? super O> parseSwitchScope(
      Element el, Class<O> outer) throws InvalidXMLException {
    outer = parseScope(el, outer, "outer");
    Class<I> inner = parseScope(el, null, "inner");

    ActionDefinition<? super I> child = parseAction(el, inner, includeObs(el, inner));

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
  public <T extends Filterable<?>> MessageAction<?> parseChatMessage(Element el, Class<T> scope)
      throws InvalidXMLException {
    Component text = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "text"));
    Component actionbar = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "actionbar"));

    Node titleNode = Node.fromChildOrAttr(el, "title");
    Node subtitleNode = Node.fromChildOrAttr(el, "subtitle");
    Title title = null;
    if (titleNode != null || subtitleNode != null)
      title = Title.title(
          XMLUtils.parseFormattedText(titleNode, empty()),
          XMLUtils.parseFormattedText(subtitleNode, empty()),
          XMLUtils.parseTitleTimes(el, Title.DEFAULT_TIMES));

    if (text == null && actionbar == null && title == null)
      throw new InvalidXMLException(
          "Expected at least one of text, title, subtitle or actionbar", el);

    List<Element> replacements = XMLUtils.flattenElements(el, "replacements");
    if (replacements.isEmpty()) {
      return new MessageAction<>(Audience.class, text, actionbar, title, null);
    }

    scope = parseScope(el, scope);

    ImmutableMap.Builder<String, MessageAction.Replacement<T>> replacementMap =
        ImmutableMap.builder();
    for (Element replacement : XMLUtils.flattenElements(el, "replacements")) {
      replacementMap.put(
          XMLUtils.parseRequiredId(replacement), parseReplacement(replacement, scope));
    }
    return new MessageAction<>(scope, text, actionbar, title, replacementMap.build());
  }

  private <T extends Filterable<?>> MessageAction.Replacement<T> parseReplacement(
      Element el, Class<T> scope) throws InvalidXMLException {
    // TODO: Support alternative replacement types (eg: player(s), team(s), or durations)
    switch (el.getName()) {
      case "decimal":
        Formula<T> formula =
            Formula.of(Node.fromRequiredAttr(el, "value").getValue(), variables.getContext(scope));
        Node formatNode = Node.fromAttr(el, "format");
        NumberFormat format =
            formatNode != null ? new DecimalFormat(formatNode.getValue()) : DEFAULT_FORMAT;
        return (T filterable) -> text(format.format(formula.applyAsDouble(filterable)));
      default:
        throw new InvalidXMLException("Unknown replacement type", el);
    }
  }

  @MethodParser("sound")
  public SoundAction parseSoundAction(Element el, Class<?> scope) throws InvalidXMLException {
    SoundType soundType =
        XMLUtils.parseEnum(Node.fromAttr(el, "preset"), SoundType.class, SoundType.CUSTOM);
    Node resourceNode = Node.fromAttr(el, "key");
    String resource = resourceNode == null ? soundType.getResource() : resourceNode.getValue();

    float volume = Math.min(
        1f, XMLUtils.parseNumber(Node.fromAttr(el, "volume"), Float.class, soundType.getVolume()));
    float pitch =
        XMLUtils.parseNumber(Node.fromAttr(el, "pitch"), Float.class, soundType.getPitch());

    Sound sound = sound(key(resource, ':'), Sound.Source.MASTER, volume, pitch);

    return new SoundAction(sound);
  }

  @MethodParser("set")
  public <T extends Filterable<?>> SetVariableAction<T> parseSetVariable(Element el, Class<T> scope)
      throws InvalidXMLException {
    var node = Node.fromRequiredAttr(el, "var");
    Variable<?> var = features.resolve(node, Variable.class);
    scope = parseScope(el, scope);

    if (!Filterables.isAssignable(scope, var.getScope()))
      throw new InvalidXMLException(
          "Wrong variable scope for '"
              + node.getValue()
              + "', expected "
              + var.getScope().getSimpleName()
              + " which cannot be found in "
              + scope.getSimpleName(),
          el);

    if (var.isReadonly())
      throw new InvalidXMLException("You may not use a read-only variable in set", el);

    Formula<T> formula =
        Formula.of(Node.fromRequiredAttr(el, "value").getValue(), variables.getContext(scope));

    if (var.isIndexed() && var instanceof Variable.Indexed<?> indexedVar) {
      Formula<T> idx =
          Formula.of(Node.fromRequiredAttr(el, "index").getValue(), variables.getContext(scope));
      return new SetVariableAction.Indexed<>(scope, indexedVar, idx, formula);
    }

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
        XMLUtils.parseBlockMaterialData(Node.fromRequiredAttr(el, "material")),
        filters.parseProperty(Node.fromAttr(el, "filter")),
        XMLUtils.parseBoolean(el.getAttribute("events"), false));
  }

  @MethodParser("take-payment")
  public Action<? super MatchPlayer> parseTakePayment(Element el, Class<?> scope)
      throws InvalidXMLException {
    Payable payable = Payable.of(ShopModule.parsePayments(el, factory.getKits()));
    if (payable.isFree()) throw new InvalidXMLException("Payment has not been defined", el);
    return new TakePaymentAction(
        payable,
        parseProperty(Node.fromChildOrAttr(el, "success-action"), MatchPlayer.class, null),
        parseProperty(Node.fromChildOrAttr(el, "fail-action"), MatchPlayer.class, null));
  }

  @MethodParser("velocity")
  public Action<? super MatchPlayer> parseVelocity(Element el, Class<?> scope)
      throws InvalidXMLException {
    Formula<MatchPlayer> xFormula = Formula.of(
        Node.fromRequiredAttr(el, "x").getValue(), variables.getContext(MatchPlayer.class));
    Formula<MatchPlayer> yFormula = Formula.of(
        Node.fromRequiredAttr(el, "y").getValue(), variables.getContext(MatchPlayer.class));
    Formula<MatchPlayer> zFormula = Formula.of(
        Node.fromRequiredAttr(el, "z").getValue(), variables.getContext(MatchPlayer.class));

    return new VelocityAction(xFormula, yFormula, zFormula);
  }

  @MethodParser("teleport")
  public Action<? super MatchPlayer> parseTeleport(Element el, Class<?> scope)
      throws InvalidXMLException {
    Formula<MatchPlayer> xFormula = Formula.of(
        Node.fromRequiredAttr(el, "x").getValue(), variables.getContext(MatchPlayer.class));
    Formula<MatchPlayer> yFormula = Formula.of(
        Node.fromRequiredAttr(el, "y").getValue(), variables.getContext(MatchPlayer.class));
    Formula<MatchPlayer> zFormula = Formula.of(
        Node.fromRequiredAttr(el, "z").getValue(), variables.getContext(MatchPlayer.class));

    Node pitchNode = Node.fromChildOrAttr(el, "pitch");
    Optional<Formula<MatchPlayer>> pitchFormula = Optional.ofNullable(
        pitchNode == null
            ? null
            : Formula.of(pitchNode.getValue(), variables.getContext(MatchPlayer.class)));

    Node yawNode = Node.fromChildOrAttr(el, "yaw");
    Optional<Formula<MatchPlayer>> yawFormula = Optional.ofNullable(
        yawNode == null
            ? null
            : Formula.of(yawNode.getValue(), variables.getContext(MatchPlayer.class)));

    return new TeleportAction(xFormula, yFormula, zFormula, pitchFormula, yawFormula);
  }

  @MethodParser("paste-structure")
  public <T extends Filterable<?>> PasteStructureAction<T> parseStructure(
      Element el, Class<T> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    Formula<T> xFormula =
        Formula.of(Node.fromRequiredAttr(el, "x").getValue(), variables.getContext(scope));
    Formula<T> yFormula =
        Formula.of(Node.fromRequiredAttr(el, "y").getValue(), variables.getContext(scope));
    Formula<T> zFormula =
        Formula.of(Node.fromRequiredAttr(el, "z").getValue(), variables.getContext(scope));

    var structure =
        features.createReference(Node.fromRequiredAttr(el, "structure"), StructureDefinition.class);

    return new PasteStructureAction<>(scope, xFormula, yFormula, zFormula, structure);
  }
}
