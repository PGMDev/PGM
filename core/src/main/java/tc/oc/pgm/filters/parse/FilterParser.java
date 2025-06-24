package tc.oc.pgm.filters.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.matcher.CauseFilter;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.block.BlocksFilter;
import tc.oc.pgm.filters.matcher.block.MaterialFilter;
import tc.oc.pgm.filters.matcher.block.StructuralLoadFilter;
import tc.oc.pgm.filters.matcher.block.VoidFilter;
import tc.oc.pgm.filters.matcher.damage.AttackerQueryModifier;
import tc.oc.pgm.filters.matcher.damage.RelationFilter;
import tc.oc.pgm.filters.matcher.damage.VictimQueryModifier;
import tc.oc.pgm.filters.matcher.entity.EntityTypeFilter;
import tc.oc.pgm.filters.matcher.entity.SpawnReasonFilter;
import tc.oc.pgm.filters.matcher.match.FlagStateFilter;
import tc.oc.pgm.filters.matcher.match.MatchPhaseFilter;
import tc.oc.pgm.filters.matcher.match.MonostableFilter;
import tc.oc.pgm.filters.matcher.match.PlayerCountFilter;
import tc.oc.pgm.filters.matcher.match.PulseFilter;
import tc.oc.pgm.filters.matcher.match.RandomFilter;
import tc.oc.pgm.filters.matcher.match.VariableFilter;
import tc.oc.pgm.filters.matcher.party.CompetitorFilter;
import tc.oc.pgm.filters.matcher.party.GoalFilter;
import tc.oc.pgm.filters.matcher.party.RankFilter;
import tc.oc.pgm.filters.matcher.party.ScoreFilter;
import tc.oc.pgm.filters.matcher.player.CanFlyFilter;
import tc.oc.pgm.filters.matcher.player.CarryingFlagFilter;
import tc.oc.pgm.filters.matcher.player.CarryingItemFilter;
import tc.oc.pgm.filters.matcher.player.EffectFilter;
import tc.oc.pgm.filters.matcher.player.FlyingFilter;
import tc.oc.pgm.filters.matcher.player.GroundedFilter;
import tc.oc.pgm.filters.matcher.player.HoldingItemFilter;
import tc.oc.pgm.filters.matcher.player.KillStreakFilter;
import tc.oc.pgm.filters.matcher.player.LivesFilter;
import tc.oc.pgm.filters.matcher.player.ParticipatingFilter;
import tc.oc.pgm.filters.matcher.player.PlayerClassFilter;
import tc.oc.pgm.filters.matcher.player.PlayerMovementFilter;
import tc.oc.pgm.filters.matcher.player.PlayerStateFilter;
import tc.oc.pgm.filters.matcher.player.WearingItemFilter;
import tc.oc.pgm.filters.modifier.LocationQueryModifier;
import tc.oc.pgm.filters.modifier.PlayerQueryModifier;
import tc.oc.pgm.filters.modifier.SameTeamQueryModifier;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
import tc.oc.pgm.filters.operator.FilterWrapper;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.operator.OneFilter;
import tc.oc.pgm.filters.operator.TeamFilterAdapter;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.post.PostDefinition;
import tc.oc.pgm.flag.state.Captured;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.flag.state.Dropped;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.flag.state.State;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.XMLParser;
import tc.oc.pgm.util.bukkit.EntityTypes;
import tc.oc.pgm.util.collection.ContextStore;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.math.OffsetVector;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLFluentParser;
import tc.oc.pgm.util.xml.XMLUtils;
import tc.oc.pgm.variables.Variable;

public abstract class FilterParser implements XMLParser<Filter, FilterDefinition> {

  protected final Map<String, Method> methodParsers;
  protected final MapFactory factory;
  protected final XMLFluentParser parser;
  protected final FeatureDefinitionContext features;

  public FilterParser(MapFactory factory) {
    this.factory = factory;
    this.parser = factory.getParser();
    this.features = factory.getFeatures();

    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  @Override
  public String type() {
    return "filter";
  }

  /**
   * Gets the context used by this parser to store filters/filter references.
   *
   * @return the context where this parser puts its parsed filters
   */
  public abstract ContextStore<? super Filter> getUsedContext();

  /**
   * The top-level method for parsing an individual filter element. This method should call
   * {@link #parseDynamic} at some point, and should also take care of adding the filter to whatever
   * type of context is in use.
   */
  public abstract Filter parse(Element el) throws InvalidXMLException;

  @Override
  public Filter parsePropertyElement(Element property) throws InvalidXMLException {
    return parseChild(property);
  }

  /**
   * Return the filter referenced by the given name/id, and assume it appears in the given
   * {@link Node} for error reporting purposes.
   */
  public abstract Filter parseReference(Node node, String id) throws InvalidXMLException;

  public boolean isFilter(Element el) {
    return methodParsers.containsKey(el.getName()) || factory.getRegions().isRegion(el);
  }

  public void parseFilterChildren(Element parent) throws InvalidXMLException {
    for (Element el : parent.getChildren()) {
      if (isFilter(el)) this.parse(el);
    }
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  protected Filter parseDynamic(Element el) throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (Filter) parser.invoke(this, el);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else if (factory.getRegions().isRegion(el)) {
      return factory.getRegions().parse(el);
    } else {
      throw new InvalidXMLException("Unknown filter type: " + el.getName(), el);
    }
  }

  protected List<Filter> parseChildren(Element parent) throws InvalidXMLException {
    List<Filter> filters = Lists.newArrayList();
    for (Element el : parent.getChildren()) {
      filters.add(this.parse(el));
    }
    return filters;
  }

  /** These "property" methods are the ones to use for parsing filters as part of other modules. */
  public @Nullable Filter parseFilterProperty(Element el, String name) throws InvalidXMLException {
    return this.parseProperty(el, name, null, null);
  }

  public Filter parseRequiredFilterProperty(Element el, String name) throws InvalidXMLException {
    return parseRequiredProperty(el, name);
  }

  public Filter parseFilterProperty(Element el, String name, @Nullable Filter def)
      throws InvalidXMLException {
    return parseProperty(el, name, def);
  }

  /**
   * @param name the attribute/child name
   * @return list with all filters defined as either attribute or child named {@param name}
   */
  public List<Filter> parseFiltersProperty(Element el, String name) throws InvalidXMLException {
    List<Filter> filters = new ArrayList<>();
    Attribute attr = el.getAttribute(name);
    if (attr != null) {
      filters.add(this.parseReference(new Node(attr)));
    }
    for (Element elFilter : el.getChildren(name)) {
      filters.addAll(this.parseChildren(elFilter));
    }
    return filters;
  }

  /** Methods for parsing specific filter types */
  @MethodParser("always")
  public Filter parseAlways(Element el) {
    return StaticFilter.ALLOW;
  }

  @MethodParser("never")
  public Filter parseNever(Element el) {
    return StaticFilter.DENY;
  }

  @MethodParser("any")
  public Filter parseAny(Element el) throws InvalidXMLException {
    return FilterWrapper.of(el, AnyFilter.of(parseChildren(el)));
  }

  @MethodParser("all")
  public Filter parseAll(Element el) throws InvalidXMLException {
    return FilterWrapper.of(el, AllFilter.of(parseChildren(el)));
  }

  @MethodParser("one")
  public Filter parseOne(Element el) throws InvalidXMLException {
    return FilterWrapper.of(el, OneFilter.of(parseChildren(el)));
  }

  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(parseChild(el));
  }

  @MethodParser("team")
  public Filter parseTeam(Element el) throws InvalidXMLException {
    return FilterWrapper.of(el, parseReference(new Node(el)));
  }

  @MethodParser("same-team")
  public SameTeamQueryModifier parseSameTeam(Element el) throws InvalidXMLException {
    return new SameTeamQueryModifier(parseChild(el));
  }

  @MethodParser("participating")
  public ParticipatingFilter parseParticipating(Element el) {
    return ParticipatingFilter.PARTICIPATING;
  }

  @MethodParser("observing")
  public ParticipatingFilter parseObserving(Element el) {
    return ParticipatingFilter.OBSERVING;
  }

  @MethodParser("attacker")
  public AttackerQueryModifier parseAttacker(Element el) throws InvalidXMLException {
    return new AttackerQueryModifier(parseChild(el));
  }

  @MethodParser("victim")
  public VictimQueryModifier parseVictim(Element el) throws InvalidXMLException {
    return new VictimQueryModifier(parseChild(el));
  }

  @MethodParser("class")
  public PlayerClassFilter parseClass(Element el) throws InvalidXMLException {
    ClassModule classes = this.factory.getModule(ClassModule.class);
    if (classes == null) {
      throw new InvalidXMLException("No classes defined", el);
    } else {
      PlayerClass playerClass =
          StringUtils.bestFuzzyMatch(el.getTextNormalize(), classes.getPlayerClasses());

      if (playerClass == null) {
        throw new InvalidXMLException("Could not find player-class: " + el.getTextNormalize(), el);
      } else {
        return new PlayerClassFilter(playerClass);
      }
    }
  }

  @MethodParser("material")
  public MaterialFilter parseMaterial(Element el) throws InvalidXMLException {
    return new MaterialFilter(MaterialMatcher.builder().parse(new Node(el)).build());
  }

  @MethodParser("void")
  public VoidFilter parseVoid(Element el) throws InvalidXMLException {
    return VoidFilter.INSTANCE;
  }

  @MethodParser("entity")
  public EntityTypeFilter parseEntity(Element el) throws InvalidXMLException {
    var type = EntityTypes.getByName(el.getTextNormalize());
    if (type == null)
      throw new InvalidXMLException("Could not find entity type: " + el.getTextNormalize(), el);

    return new EntityTypeFilter(type);
  }

  @MethodParser("mob")
  public EntityTypeFilter parseMob(Element el) throws InvalidXMLException {
    EntityTypeFilter matcher = this.parseEntity(el);
    if (!LivingEntity.class.isAssignableFrom(matcher.getEntityType())) {
      throw new InvalidXMLException("Unknown mob type: " + el.getTextNormalize(), el);
    }
    return matcher;
  }

  @MethodParser("spawn")
  public SpawnReasonFilter parseSpawnReason(Element el) throws InvalidXMLException {
    return new SpawnReasonFilter(XMLUtils.parseEnum(new Node(el), SpawnReason.class));
  }

  @MethodParser("kill-streak")
  public KillStreakFilter parseKillStreak(Element el) throws InvalidXMLException {
    boolean repeat = XMLUtils.parseBoolean(el.getAttribute("repeat"), false);
    Range<Integer> range = XMLUtils.parseNumericRange(el, Integer.class);

    if (!range.hasUpperBound() && repeat) {
      throw new InvalidXMLException(
          "kill-streak filters with repeat=\"true\" must define a max or count", el);
    }

    return new KillStreakFilter(range, repeat);
  }

  @MethodParser("lives")
  public LivesFilter parseLives(Element el) throws InvalidXMLException {
    return new LivesFilter(XMLUtils.parseNumericRange(el, Integer.class));
  }

  @MethodParser("random")
  public RandomFilter parseRandom(Element el) throws InvalidXMLException {
    Node node = new Node(el);
    Range<Double> chance;
    try {
      chance = Range.closedOpen(0d, XMLUtils.parseNumber(node, Double.class));
    } catch (InvalidXMLException e) {
      chance = XMLUtils.parseNumericRange(node, Double.class);
    }

    Range<Double> valid = Range.closed(0d, 1d);
    if (valid.encloses(chance)) {
      return new RandomFilter(chance);
    } else {
      double lower = chance.hasLowerBound() ? chance.lowerEndpoint() : Double.NEGATIVE_INFINITY;
      double upper = chance.hasUpperBound() ? chance.upperEndpoint() : Double.POSITIVE_INFINITY;
      double invalid;
      if (!valid.contains(lower)) {
        invalid = lower;
      } else {
        invalid = upper;
      }

      throw new InvalidXMLException("chance value (" + invalid + ") is not between 0 and 1", el);
    }
  }

  @MethodParser("crouching")
  public PlayerMovementFilter parseCrouching(Element el) throws InvalidXMLException {
    return PlayerMovementFilter.CROUCHING;
  }

  @MethodParser("walking")
  public PlayerMovementFilter parseWalking(Element el) throws InvalidXMLException {
    return PlayerMovementFilter.WALKING;
  }

  @MethodParser("sprinting")
  public PlayerMovementFilter parseSprinting(Element el) throws InvalidXMLException {
    return PlayerMovementFilter.SPRINTING;
  }

  @MethodParser("flying")
  public FlyingFilter parseFlying(Element el) throws InvalidXMLException {
    return FlyingFilter.INSTANCE;
  }

  @MethodParser("can-fly")
  public CanFlyFilter parseCanFly(Element el) throws InvalidXMLException {
    return CanFlyFilter.INSTANCE;
  }

  @MethodParser("grounded")
  public GroundedFilter parseGrounded(Element el) throws InvalidXMLException {
    return GroundedFilter.INSTANCE;
  }

  @MethodParser("alive")
  public PlayerStateFilter parseAlive(Element el) throws InvalidXMLException {
    return PlayerStateFilter.ALIVE;
  }

  @MethodParser("dead")
  public PlayerStateFilter parseDead(Element el) throws InvalidXMLException {
    return PlayerStateFilter.DEAD;
  }

  private Filter parseExplicitTeam(Element el, CompetitorFilter filter) throws InvalidXMLException {
    final boolean any = XMLUtils.parseBoolean(el.getAttribute("any"), false);
    final Optional<XMLFeatureReference<TeamFactory>> team = Optional.ofNullable(
            Node.fromAttr(el, "team"))
        .map(n -> features.createReference(n, TeamFactory.class));

    if (any && team.isPresent())
      throw new InvalidXMLException("Cannot combine attributes 'team' and 'any'", el);

    return any || team.isPresent() ? new TeamFilterAdapter(team, filter) : filter;
  }

  private GoalFilter goalFilter(Element el) throws InvalidXMLException {
    return new GoalFilter(features.createReference(new Node(el), GoalDefinition.class));
  }

  @MethodParser("objective")
  public Filter parseGoal(Element el) throws InvalidXMLException {
    return parseExplicitTeam(el, goalFilter(el));
  }

  @MethodParser("completed")
  public Filter parseCompleted(Element el) throws InvalidXMLException {
    return new TeamFilterAdapter(Optional.empty(), goalFilter(el));
  }

  @MethodParser("captured")
  public Filter parseCaptured(Element el) throws InvalidXMLException {
    return parseExplicitTeam(el, goalFilter(el));
  }

  protected FlagStateFilter parseFlagState(Element el, Class<? extends State> state)
      throws InvalidXMLException {
    Node postAttr = Node.fromAttr(el, "post");
    return new FlagStateFilter(
        features.createReference(new Node(el), FlagDefinition.class),
        postAttr == null ? null : features.createReference(postAttr, PostDefinition.class),
        state);
  }

  @MethodParser("flag-carried")
  public FlagStateFilter parseFlagCarried(Element el) throws InvalidXMLException {
    return this.parseFlagState(el, Carried.class);
  }

  @MethodParser("flag-dropped")
  public FlagStateFilter parseFlagDropped(Element el) throws InvalidXMLException {
    return this.parseFlagState(el, Dropped.class);
  }

  @MethodParser("flag-returned")
  public FlagStateFilter parseFlagReturned(Element el) throws InvalidXMLException {
    return this.parseFlagState(el, Returned.class);
  }

  @MethodParser("flag-captured")
  public FlagStateFilter parseFlagCaptured(Element el) throws InvalidXMLException {
    return this.parseFlagState(el, Captured.class);
  }

  @MethodParser("carrying-flag")
  public CarryingFlagFilter parseCarryingFlag(Element el) throws InvalidXMLException {
    return new CarryingFlagFilter(features.createReference(new Node(el), FlagDefinition.class));
  }

  @MethodParser("cause")
  public CauseFilter parseCause(Element el) throws InvalidXMLException {
    return new CauseFilter(XMLUtils.parseEnum(el, CauseFilter.Cause.class));
  }

  @MethodParser("relation")
  public RelationFilter parseRelation(Element el) throws InvalidXMLException {
    return new RelationFilter(XMLUtils.parseEnum(el, PlayerRelation.class));
  }

  @MethodParser("carrying")
  public CarryingItemFilter parseHasItem(Element el) throws InvalidXMLException {
    return new CarryingItemFilter(factory.getKits().parseItemMatcher(el));
  }

  @MethodParser("holding")
  public HoldingItemFilter parseHolding(Element el) throws InvalidXMLException {
    return new HoldingItemFilter(factory.getKits().parseItemMatcher(el));
  }

  @MethodParser("wearing")
  public WearingItemFilter parseWearingItem(Element el) throws InvalidXMLException {
    return new WearingItemFilter(factory.getKits().parseItemMatcher(el));
  }

  @MethodParser("effect")
  public EffectFilter parseEffect(Element el) throws InvalidXMLException {
    Duration minDuration = XMLUtils.parseDuration(Node.fromAttr(el, "min-duration"));
    Duration maxDuration = XMLUtils.parseDuration(Node.fromAttr(el, "max-duration"));
    Range<Integer> duration;
    if (minDuration == null && maxDuration == null) {
      duration = Range.all();
    } else if (minDuration == null) {
      duration = Range.atMost((int) (maxDuration.getSeconds() * 20));
    } else if (maxDuration == null) {
      duration = Range.atLeast((int) (minDuration.getSeconds() * 20));
    } else {
      duration = Range.closed(
          (int) (minDuration.getSeconds() * 20), (int) (maxDuration.getSeconds() * 20));
    }
    boolean amplifier = Node.fromAttr(el, "amplifier") != null;
    return new EffectFilter(XMLUtils.parsePotionEffect(el), duration, amplifier);
  }

  @MethodParser("structural-load")
  public StructuralLoadFilter parseStructuralLoad(Element el) throws InvalidXMLException {
    return new StructuralLoadFilter(XMLUtils.parseNumber(el, Integer.class));
  }

  @MethodParser("time")
  public Filter parseTimeFilter(Element el) throws InvalidXMLException {
    return MonostableFilter.afterMatchStart(XMLUtils.parseDuration(el, null));
  }

  @MethodParser("countdown")
  public Filter parseCountdownFilter(Element el) throws InvalidXMLException {
    Duration duration = XMLUtils.parseDuration(Node.fromRequiredAttr(el, "duration"));
    if (duration.isNegative() || duration.isZero()) return StaticFilter.DENY;

    Filter child = parseProperty(Node.fromAttrOrSelf(el, "filter"), DynamicFilterValidation.ANY);
    Component message = XMLUtils.parseFormattedText(el, "message", null);
    return new MonostableFilter(child, duration, message);
  }

  @MethodParser("after")
  public Filter parseAfterFilter(Element el) throws InvalidXMLException {
    Duration duration = XMLUtils.parseDuration(Node.fromRequiredAttr(el, "duration"));
    Filter child = parseProperty(Node.fromAttrOrSelf(el, "filter"), DynamicFilterValidation.ANY);
    if (duration.isNegative() || duration.isZero()) return child;

    Component message = XMLUtils.parseFormattedText(el, "message", null);
    return MonostableFilter.after(child, duration, message);
  }

  @MethodParser("pulse")
  public Filter parsePulseFilter(Element el) throws InvalidXMLException {
    Duration duration = XMLUtils.parseDuration(Node.fromRequiredAttr(el, "duration"));
    Duration period = XMLUtils.parseDuration(Node.fromRequiredAttr(el, "period"));

    long durationTicks = TimeUtils.toTicks(duration);
    long periodTicks = TimeUtils.toTicks(period);
    if (durationTicks <= 0) throw new InvalidXMLException("Duration must be positive", el);
    if (durationTicks >= periodTicks)
      throw new InvalidXMLException("Duration in ticks must be smaller than period in ticks.", el);

    Filter child = parseProperty(Node.fromAttrOrSelf(el, "filter"), DynamicFilterValidation.ANY);
    return new PulseFilter(child, durationTicks, periodTicks);
  }

  @MethodParser("rank")
  public Filter parseRankFilter(Element el) throws InvalidXMLException {
    return parseExplicitTeam(
        el, new RankFilter(XMLUtils.parseNumericRange(new Node(el), Integer.class)));
  }

  @MethodParser("score")
  public Filter parseScoreFilter(Element el) throws InvalidXMLException {
    return parseExplicitTeam(
        el, new ScoreFilter(XMLUtils.parseNumericRange(new Node(el), Integer.class)));
  }

  @MethodParser("match-phase")
  public Filter parseMatchPhase(Element el) throws InvalidXMLException {
    return switch (el.getTextNormalize()) {
      case "running" -> MatchPhaseFilter.RUNNING;
      case "finished" -> MatchPhaseFilter.FINISHED;
      case "starting" -> MatchPhaseFilter.STARTING;
      case "idle" -> MatchPhaseFilter.IDLE;
      case "started" -> MatchPhaseFilter.STARTED;
      default -> throw new InvalidXMLException("Invalid or no match state found", el);
    };
  }

  @MethodParser("match-started")
  public Filter parseMatchStarted(Element el) throws InvalidXMLException {
    return MatchPhaseFilter.STARTED;
  }

  @MethodParser("match-running")
  public Filter parseMatchRunning(Element el) throws InvalidXMLException {
    return MatchPhaseFilter.RUNNING;
  }

  @MethodParser("match-finished")
  public Filter parseMatchFinished(Element el) throws InvalidXMLException {
    return MatchPhaseFilter.FINISHED;
  }

  // Methods for parsing QueryModifiers

  @MethodParser("offset")
  public Filter parseOffsetFilter(Element el) throws InvalidXMLException {
    OffsetVector vector = XMLUtils.parseOffsetVector(Node.fromRequiredAttr(el, "vector"));
    Filter child = parseProperty(Node.fromAttrOrSelf(el, "filter"));

    return LocationQueryModifier.of(child, vector);
  }

  @MethodParser("player")
  public PlayerQueryModifier parsePlayerFilter(Element el) throws InvalidXMLException {
    return new PlayerQueryModifier(parseChild(el));
  }

  @MethodParser("players")
  public PlayerCountFilter parsePlayerCountFilter(Element el) throws InvalidXMLException {
    Filter child = parseProperty(
        Node.fromAttrOrSelf(el, "filter"), StaticFilter.ALLOW, DynamicFilterValidation.PLAYER);

    return new PlayerCountFilter(
        child,
        XMLUtils.parseNumericRange(el, Integer.class, Range.atLeast(1)),
        XMLUtils.parseBoolean(el.getAttribute("participants"), true),
        XMLUtils.parseBoolean(el.getAttribute("observers"), false));
  }

  @MethodParser("variable")
  public Filter parseVariableFilter(Element el) throws InvalidXMLException {
    Range<Double> range = XMLUtils.parseNumericRange(new Node(el), Double.class);

    Filter filter;
    if (el.getAttribute("var") != null) {
      Variable<?> varDef = parser.variable(el, "var").required();
      Integer index = varDef.isIndexed() ? parser.parseInt(el, "index").required() : null;

      filter = VariableFilter.of(varDef, index, range, new Node(el));
    } else if (el.getAttribute("value") != null) {
      var scope = Filterables.parse(Node.fromRequiredAttr(el, "scope"));
      var formula = parser.formula(scope, el, "value").attr().required();

      filter = VariableFilter.of(formula, scope, range);
    } else {
      throw new InvalidXMLException("Expected one of 'var' or 'value'", el);
    }

    return filter instanceof CompetitorFilter
        ? parseExplicitTeam(el, (CompetitorFilter) filter)
        : filter;
  }

  @MethodParser("blocks")
  public Filter parseBlocksFilter(Element el) throws InvalidXMLException {
    Region region =
        factory.getRegions().parseRequiredProperty(el, "region", BlockBoundedValidation.INSTANCE);
    Filter child = parseProperty(Node.fromAttrOrSelf(el, "filter"), MaterialFilter.NOT_AIR);

    return new BlocksFilter(region, child);
  }
}
