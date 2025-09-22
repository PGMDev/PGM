package tc.oc.pgm.action;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.empty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.actions.ActionNode;
import tc.oc.pgm.action.actions.DropFlagAction;
import tc.oc.pgm.action.actions.EnchantItemAction;
import tc.oc.pgm.action.actions.ExposedAction;
import tc.oc.pgm.action.actions.FillAction;
import tc.oc.pgm.action.actions.KillEntitiesAction;
import tc.oc.pgm.action.actions.MessageAction;
import tc.oc.pgm.action.actions.PasteStructureAction;
import tc.oc.pgm.action.actions.PickupFlagAction;
import tc.oc.pgm.action.actions.RepeatAction;
import tc.oc.pgm.action.actions.ReplaceItemAction;
import tc.oc.pgm.action.actions.ScopeSwitchAction;
import tc.oc.pgm.action.actions.SetVariableAction;
import tc.oc.pgm.action.actions.SoundAction;
import tc.oc.pgm.action.actions.TakePaymentAction;
import tc.oc.pgm.action.actions.TeamAliasAction;
import tc.oc.pgm.action.actions.TeleportAction;
import tc.oc.pgm.action.actions.VelocityAction;
import tc.oc.pgm.action.actions.WeatherAction;
import tc.oc.pgm.action.replacements.Replacement;
import tc.oc.pgm.action.replacements.ReplacementParser;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.player.ParticipatingFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.modules.WeatherMatchModule;
import tc.oc.pgm.shops.ShopModule;
import tc.oc.pgm.shops.menu.Payable;
import tc.oc.pgm.structure.StructureDefinition;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.inventory.ItemMatcher;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariableParser;
import tc.oc.pgm.variables.VariablesModule;

public class ActionParser {

  private final MapFactory factory;
  private final boolean legacy;
  private final FeatureDefinitionContext features;
  private final XMLFluentParser parser;
  private final Map<String, Method> methodParsers;
  private final ReplacementParser replacementParser;

  public ActionParser(MapFactory factory) {
    this.factory = factory;
    this.legacy = !factory.getProto().isNoOlderThan(MapProtos.ACTION_REVAMP);
    this.features = factory.getFeatures();
    this.parser = factory.getParser();
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
    replacementParser = new ReplacementParser(factory);
  }

  public <B extends Filterable<?>> Action<? super B> parseProperty(
      Element el, @Nullable Class<B> bound) throws InvalidXMLException {
    return parse(el, bound, true);
  }

  public <B extends Filterable<?>> Action<? super B> parse(Element el, @Nullable Class<B> bound)
      throws InvalidXMLException {
    return parse(el, bound, false);
  }

  @SuppressWarnings("unchecked")
  private <B extends Filterable<?>> Action<? super B> parse(
      Element el, @Nullable Class<B> bound, boolean property) throws InvalidXMLException {
    String id = FeatureDefinitionContext.parseId(el);

    Node node = new Node(el);
    if (id != null && maybeReference(el, property)) {
      return parseReference(node, id, bound);
    }

    Action<? super B> result = property ? parseAction(el, bound) : parseDynamic(el, bound);
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

  private boolean maybeReference(Element el, boolean property) {
    return (property || "action".equals(el.getName())) && el.getChildren().isEmpty();
  }

  public <B extends Filterable<?>> Action<? super B> parseReference(Node node, Class<B> bound)
      throws InvalidXMLException {
    return parseReference(node, node.getValue(), bound);
  }

  private <B extends Filterable<?>> Action<? super B> parseReference(
      Node node, String id, Class<B> bound) throws InvalidXMLException {
    @SuppressWarnings("unchecked")
    var action = (Action<? super B>) features.get(id, ActionDefinition.class);
    if (action == null) action = parseInlineAction(node, id, bound);
    if (action == null)
      action = features.addReference(new XMLActionReference<>(features, node, id));

    validate(action, ActionScopeValidation.of(bound), node);
    return action;
  }

  private static final String EXPRESSION = "[^=]+";
  private static final Pattern INLINE_SET =
      Pattern.compile("(%VAR%)(?:\\[(%IDX%)])?\\s*:=\\s*(%EXP%)"
          .replace("%VAR%", VariableParser.VARIABLE_ID.pattern())
          .replace("%IDX%", EXPRESSION)
          .replace("%EXP%", EXPRESSION));

  private <B extends Filterable<?>> Action<? super B> parseInlineAction(
      Node node, String id, Class<B> scope) throws InvalidXMLException {
    Matcher match = INLINE_SET.matcher(id);
    if (!match.matches()) return null;

    if (scope == null)
      throw new InvalidXMLException("Inline action requires an implicit scope", node);

    var context = factory.needModule(VariablesModule.class).getContext(scope);

    Variable<?> var = features.resolve(node, match.group(1), Variable.class);
    Formula<B> formula = Formula.of(match.group(3), context);

    if (var.isReadonly())
      throw new InvalidXMLException("Variable was readonly when write access is required", node);

    if (var.isIndexed() && var instanceof Variable.Indexed<?> varIdx) {
      var idxText = match.group(2);
      if (idxText == null)
        throw new InvalidXMLException(
            "Inline action doesn't define the index to insert into", node);

      return new SetVariableAction.Indexed<>(scope, varIdx, Formula.of(idxText, context), formula);
    }
    return new SetVariableAction<>(scope, var, formula);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void validate(
      Action<?> action, FeatureValidation<ActionDefinition<?>> validation, Node node)
      throws InvalidXMLException {
    if (action instanceof XMLFeatureReference ref) {
      features.validate(ref, validation);
    } else if (action instanceof ActionDefinition ad) {
      features.validate(ad, validation, node);
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
        wrapFilter(parser.filter(el, "filter").dynamic(cls).required(), includeObs(el, cls)),
        parser.action(cls, el, "action", "trigger").required());
  }

  // Generic action with N children parser
  private <B extends Filterable<?>> ActionNode<? super B> parseAction(
      Element el, Class<B> scope, boolean obs) throws InvalidXMLException {
    scope = parseScope(el, scope);

    if (el.getChildren().isEmpty())
      throw new InvalidXMLException("No action children were defined", el);

    ImmutableList.Builder<Action<? super B>> children = ImmutableList.builder();
    for (Element child : el.getChildren()) {
      children.add(parse(child, scope));
    }

    Filter filter = parser.filter(el, "filter").orAllow();
    Filter untriggerFilter =
        parser.filter(el, "untrigger-filter").result(!legacy && filter == StaticFilter.ALLOW);

    return new ActionNode<>(
        children.build(), wrapFilter(filter, obs), wrapFilter(untriggerFilter, obs), scope);
  }

  // Parsers
  @MethodParser("action")
  public <B extends Filterable<?>> ActionNode<? super B> parseAction(Element el, Class<B> scope)
      throws InvalidXMLException {
    return parseAction(el, scope, true);
  }

  @MethodParser("repeat")
  public <B extends Filterable<?>> Action<? super B> parseRepeat(Element el, Class<B> scope)
      throws InvalidXMLException {
    scope = parseScope(el, scope);

    Action<? super B> child = parseAction(el, scope, true);
    Formula<B> formula = parser.formula(scope, el, "times").required();

    return new RepeatAction<>(scope, child, formula);
  }

  @MethodParser("switch-scope")
  public <O extends Filterable<?>, I extends Filterable<?>> Action<? super O> parseSwitchScope(
      Element el, Class<O> outer) throws InvalidXMLException {
    outer = parseScope(el, outer, "outer");
    Class<I> inner = parseScope(el, null, "inner");

    Action<? super I> child = parseAction(el, inner, includeObs(el, inner));

    Action<? super O> result = ScopeSwitchAction.of(child, outer, inner);
    if (result == null) {
      throw new InvalidXMLException(
          "Could not convert from " + outer.getSimpleName() + " to " + inner.getSimpleName(), el);
    }
    return result;
  }

  @MethodParser("kit")
  public Kit parseKitTrigger(Element el, Class<?> scope) throws InvalidXMLException {
    return parser.kit(el).required();
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
      return new MessageAction<>(Filterable.class, text, actionbar, title, null);
    }

    scope = parseScope(el, scope);

    ImmutableMap.Builder<String, Replacement> replacementMap = ImmutableMap.builder();
    for (Element replacement : XMLUtils.flattenElements(el, "replacements")) {
      replacementMap.put(
          XMLUtils.parseRequiredId(replacement), replacementParser.parse(replacement, scope));
    }
    return new MessageAction<>(scope, text, actionbar, title, replacementMap.build());
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
    scope = parseScope(el, scope);
    Variable<?> var = parser.variable(el, "var").bound(scope).writtable().required();
    Formula<T> formula = parser.formula(scope, el, "value").required();

    if (var.isIndexed() && var instanceof Variable.Indexed<?> indexedVar) {
      Formula<T> idx = parser.formula(scope, el, "index").required();
      return new SetVariableAction.Indexed<>(scope, indexedVar, idx, formula);
    }

    return new SetVariableAction<>(scope, var, formula);
  }

  @MethodParser("kill-entities")
  public KillEntitiesAction parseKillEntities(Element el, Class<?> scope)
      throws InvalidXMLException {
    return new KillEntitiesAction(parser.filter(el, "filter").required());
  }

  @MethodParser("replace-item")
  public ReplaceItemAction parseReplaceItem(Element el, Class<?> scope) throws InvalidXMLException {
    ItemMatcher matcher = factory.getKits().parseItemMatcher(el, "find");
    ItemStack item = parser.item(el, "replace").allowAir().orNull();

    boolean keepAmount = parser.parseBool(el, "keep-amount").orFalse();
    boolean keepEnchants = parser.parseBool(el, "keep-enchants").orFalse();

    return new ReplaceItemAction(matcher, item, keepAmount, keepEnchants);
  }

  @MethodParser("enchant-item")
  public EnchantItemAction parseEnchantItem(Element el, Class<?> scope) throws InvalidXMLException {
    ItemMatcher matcher = factory.getKits().parseItemMatcher(el, "find");
    Enchantment enchant = XMLUtils.parseEnchantment(Node.fromRequiredAttr(el, "enchantment"));
    Formula<MatchPlayer> level = parser.formula(MatchPlayer.class, el, "level").required();

    return new EnchantItemAction(matcher, enchant, level);
  }

  @MethodParser("fill")
  public FillAction parseFill(Element el, Class<?> scope) throws InvalidXMLException {
    return new FillAction(
        parser.region(el, "region").blockBounded().orSelf(),
        XMLUtils.parseBlockMaterialData(Node.fromRequiredAttr(el, "material")),
        parser.filter(el, "filter").orNull(),
        parser.parseBool(el, "update").orTrue(),
        parser.parseBool(el, "events").orFalse());
  }

  @MethodParser("team-alias")
  public <T extends Filterable<?>> Action<?> parseTeamAliasAction(Element el, Class<T> scope)
      throws InvalidXMLException {
    String alias = parser.string(el, "alias").required();
    var action = new TeamAliasAction(alias);
    var teamBuilder = parser.reference(TeamFactory.class, el, "team");
    var team = scope == Party.class ? teamBuilder.orNull() : teamBuilder.required();

    return team == null
        ? action
        : new ScopeSwitchAction<>(
            scope, f -> f.moduleRequire(TeamMatchModule.class).getTeam(team.get()), null, action);
  }

  @MethodParser("take-payment")
  public Action<? super MatchPlayer> parseTakePayment(Element el, Class<?> scope)
      throws InvalidXMLException {
    Payable payable = Payable.of(ShopModule.parsePayments(el, factory.getParser()));
    if (payable.isFree()) throw new InvalidXMLException("Payment has not been defined", el);
    return new TakePaymentAction(
        payable,
        parser.action(MatchPlayer.class, el, "success-action").orNull(),
        parser.action(MatchPlayer.class, el, "fail-action").orNull());
  }

  @MethodParser("velocity")
  public Action<? super MatchPlayer> parseVelocity(Element el, Class<?> scope)
      throws InvalidXMLException {
    var xFormula = parser.formula(MatchPlayer.class, el, "x").required();
    var yFormula = parser.formula(MatchPlayer.class, el, "y").required();
    var zFormula = parser.formula(MatchPlayer.class, el, "z").required();

    return new VelocityAction(xFormula, yFormula, zFormula);
  }

  @MethodParser("teleport")
  public Action<? super MatchPlayer> parseTeleport(Element el, Class<?> scope)
      throws InvalidXMLException {
    var xFormula = parser.formula(MatchPlayer.class, el, "x").required();
    var yFormula = parser.formula(MatchPlayer.class, el, "y").required();
    var zFormula = parser.formula(MatchPlayer.class, el, "z").required();

    var pitchFormula = parser.formula(MatchPlayer.class, el, "pitch").optional();
    var yawFormula = parser.formula(MatchPlayer.class, el, "yaw").optional();

    return new TeleportAction(xFormula, yFormula, zFormula, pitchFormula, yawFormula);
  }

  @MethodParser("paste-structure")
  public <T extends Filterable<?>> PasteStructureAction<T> parseStructure(
      Element el, Class<T> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var xFormula = parser.formula(scope, el, "x").required();
    var yFormula = parser.formula(scope, el, "y").required();
    var zFormula = parser.formula(scope, el, "z").required();

    var structure = parser.reference(StructureDefinition.class, el, "structure").required();
    var update = parser.parseBool(el, "update").orTrue();

    return new PasteStructureAction<>(scope, xFormula, yFormula, zFormula, structure, update);
  }

  @MethodParser("weather")
  public WeatherAction parseWeather(Element el, Class<?> scope) throws InvalidXMLException {
    return WeatherAction.of(
        parser.parseEnum(WeatherMatchModule.WeatherType.class, el, "state").required());
  }

  @MethodParser("drop-flag")
  public DropFlagAction parseDropFlag(Element el, Class<?> scope) throws InvalidXMLException {
    return new DropFlagAction(parser.reference(FlagDefinition.class, el, "flag").required());
  }

  @MethodParser("pickup-flag")
  public PickupFlagAction parsePickupFlag(Element el, Class<?> scope) throws InvalidXMLException {
    return new PickupFlagAction(
        parser.reference(FlagDefinition.class, el, "flag").required());
  }
}
