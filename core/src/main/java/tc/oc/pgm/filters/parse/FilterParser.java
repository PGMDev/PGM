package tc.oc.pgm.filters.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.filters.matcher.CauseFilter;
import tc.oc.pgm.filters.matcher.StaticFilter;
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
import tc.oc.pgm.filters.matcher.match.RandomFilter;
import tc.oc.pgm.filters.matcher.party.CompetitorFilter;
import tc.oc.pgm.filters.matcher.party.GoalFilter;
import tc.oc.pgm.filters.matcher.party.RankFilter;
import tc.oc.pgm.filters.matcher.party.ScoreFilter;
import tc.oc.pgm.filters.matcher.party.TeamFilter;
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
import tc.oc.pgm.filters.matcher.player.WearingItemFilter;
import tc.oc.pgm.filters.modifier.LocationQueryModifier;
import tc.oc.pgm.filters.modifier.PlayerBlockQueryModifier;
import tc.oc.pgm.filters.modifier.SameTeamQueryModifier;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
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
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.collection.ContextStore;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class FilterParser {

  protected final Map<String, Method> methodParsers;
  protected final MapFactory factory;
  protected final TeamModule teamModule;

  public FilterParser(MapFactory factory) {
    this.factory = factory;
    this.teamModule = factory.getModule(TeamModule.class);

    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  /**
   * Gets the context used by this parser to store filters/filter references.
   *
   * @return the context where this parser puts its parsed filters
   */
  public abstract ContextStore<? super Filter> getUsedContext();

  /**
   * The top-level method for parsing an individual filter element. This method should call {@link
   * #parseDynamic} at some point, and should also take care of adding the filter to whatever type
   * of context is in use.
   */
  public abstract Filter parse(Element el) throws InvalidXMLException;

  /**
   * Return the filter referenced by the given name/id, and assume it appears in the given {@link
   * Node} for error reporting purposes.
   */
  public abstract Filter parseReference(Node node, String value) throws InvalidXMLException;

  public Filter parseReference(Node node) throws InvalidXMLException {
    return parseReference(node, node.getValue());
  }

  public boolean isFilter(Element el) {
    return methodParsers.containsKey(el.getName()) || factory.getRegions().isRegion(el);
  }

  public List<Element> getFilterChildren(Element parent) {
    List<Element> elements = new ArrayList<Element>();
    for (Element el : parent.getChildren()) {
      if (this.isFilter(el)) {
        elements.add(el);
      }
    }
    return elements;
  }

  public List<Filter> parseFilterChildren(Element parent) throws InvalidXMLException {
    List<Filter> filters = new ArrayList<>();
    for (Element el : getFilterChildren(parent)) {
      filters.add(this.parse(el));
    }
    return filters;
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

  public Filter parseChild(Element parent) throws InvalidXMLException {
    if (parent.getChildren().isEmpty()) {
      throw new InvalidXMLException("Expected a child filter", parent);
    } else if (parent.getChildren().size() > 1) {
      throw new InvalidXMLException("Expected only one child filter, not multiple", parent);
    }
    return this.parse(parent.getChildren().get(0));
  }

  public List<Filter> parseChildren(Element parent) throws InvalidXMLException {
    List<Filter> filters = Lists.newArrayList();
    for (Element el : parent.getChildren()) {
      filters.add(this.parse(el));
    }
    return filters;
  }

  /** These "property" methods are the ones to use for parsing filters as part of other modules. */
  public @Nullable Filter parseFilterProperty(Element el, String name) throws InvalidXMLException {
    return this.parseFilterProperty(el, name, null);
  }

  public Filter parseRequiredFilterProperty(Element el, String name) throws InvalidXMLException {
    Filter filter = this.parseFilterProperty(el, name);
    if (filter == null) throw new InvalidXMLException("Missing required filter '" + name + "'", el);
    return filter;
  }

  public Filter parseFilterProperty(Element el, String name, @Nullable Filter def)
      throws InvalidXMLException {
    Attribute attr = el.getAttribute(name);
    Element child = XMLUtils.getUniqueChild(el, name);
    if (attr != null) {
      if (child != null) {
        throw new InvalidXMLException(
            "Filter reference conflicts with inline filter '" + name + "'", el);
      }
      return this.parseReference(new Node(attr));
    } else if (child != null) {
      return this.parseChild(child);
    }
    return def;
  }

  /**
   * Return a list containing any and all of the following: - A filter reference in an attribute of
   * the given name - Inline filters inside child tags of the given name
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
    return new AnyFilter(parseChildren(el));
  }

  @MethodParser("all")
  public Filter parseAll(Element el) throws InvalidXMLException {
    return new AllFilter(parseChildren(el));
  }

  @MethodParser("one")
  public Filter parseOne(Element el) throws InvalidXMLException {
    return new OneFilter(parseChildren(el));
  }

  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(AnyFilter.of(parseChildren(el)));
  }

  @MethodParser("team")
  public TeamFilter parseTeam(Element el) throws InvalidXMLException {
    return new TeamFilter(Teams.getTeamRef(new Node(el), this.factory));
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
          StringUtils.bestFuzzyMatch(el.getTextNormalize(), classes.getPlayerClasses(), 0.9);

      if (playerClass == null) {
        throw new InvalidXMLException("Could not find player-class: " + el.getTextNormalize(), el);
      } else {
        return new PlayerClassFilter(playerClass);
      }
    }
  }

  @MethodParser("material")
  public MaterialFilter parseMaterial(Element el) throws InvalidXMLException {
    return new MaterialFilter(XMLUtils.parseMaterialPattern(el));
  }

  @MethodParser("void")
  public VoidFilter parseVoid(Element el) throws InvalidXMLException {
    return new VoidFilter();
  }

  @MethodParser("entity")
  public EntityTypeFilter parseEntity(Element el) throws InvalidXMLException {
    return new EntityTypeFilter(XMLUtils.parseEnum(el, EntityType.class, "entity type"));
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
    return new SpawnReasonFilter(
        XMLUtils.parseEnum(new Node(el), SpawnReason.class, "spawn reason"));
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
    return new PlayerMovementFilter(false, true);
  }

  @MethodParser("walking")
  public PlayerMovementFilter parseWalking(Element el) throws InvalidXMLException {
    return new PlayerMovementFilter(false, false);
  }

  @MethodParser("sprinting")
  public PlayerMovementFilter parseSprinting(Element el) throws InvalidXMLException {
    return new PlayerMovementFilter(true, false);
  }

  @MethodParser("flying")
  public FlyingFilter parseFlying(Element el) throws InvalidXMLException {
    return new FlyingFilter();
  }

  @MethodParser("can-fly")
  public CanFlyFilter parseCanFly(Element el) throws InvalidXMLException {
    return new CanFlyFilter();
  }

  @MethodParser("grounded")
  public GroundedFilter parseGrounded(Element el) throws InvalidXMLException {
    return GroundedFilter.INSTANCE;
  }

  private <T extends FeatureDefinition> XMLFeatureReference<T> reference(Node node, Class<T> cls) {
    return this.factory.getFeatures().createReference(node, cls);
  }

  private <T extends FeatureDefinition> Optional<XMLFeatureReference<T>> optionalReference(
      Node node, Class<T> cls) {
    return node == null
        ? Optional.empty()
        : Optional.of(this.factory.getFeatures().createReference(node, cls));
  }

  private Filter parseExplicitTeam(Element el, CompetitorFilter filter) throws InvalidXMLException {
    final boolean any = XMLUtils.parseBoolean(el.getAttribute("any"), false);
    final Optional<XMLFeatureReference<TeamFactory>> team =
        optionalReference(Node.fromAttr(el, "team"), TeamFactory.class);

    if (any && team.isPresent())
      throw new InvalidXMLException("Cannot combine attributes 'team' and 'any'", el);

    return any || team.isPresent() ? new TeamFilterAdapter(team, filter) : filter;
  }

  private GoalFilter goalFilter(Element el) throws InvalidXMLException {
    return new GoalFilter(reference(new Node(el), GoalDefinition.class));
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
    final GoalFilter goal = goalFilter(el);
    Optional<XMLFeatureReference<TeamFactory>> team =
        optionalReference(Node.fromAttr(el, "team"), TeamFactory.class);
    return team.isPresent() ? new TeamFilterAdapter(team, goal) : goal;
  }

  protected FlagStateFilter parseFlagState(Element el, Class<? extends State> state)
      throws InvalidXMLException {
    Node postAttr = Node.fromAttr(el, "post");
    return new FlagStateFilter(
        this.factory.getFeatures().createReference(new Node(el), FlagDefinition.class),
        postAttr == null
            ? null
            : this.factory.getFeatures().createReference(postAttr, PostDefinition.class),
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
    return new CarryingFlagFilter(
        this.factory.getFeatures().createReference(new Node(el), FlagDefinition.class));
  }

  @MethodParser("cause")
  public CauseFilter parseCause(Element el) throws InvalidXMLException {
    return new CauseFilter(XMLUtils.parseEnum(el, CauseFilter.Cause.class, "cause filter"));
  }

  @MethodParser("relation")
  public RelationFilter parseRelation(Element el) throws InvalidXMLException {
    return new RelationFilter(
        XMLUtils.parseEnum(el, PlayerRelation.class, "player relation filter"));
  }

  @MethodParser("carrying")
  public CarryingItemFilter parseHasItem(Element el) throws InvalidXMLException {
    return new CarryingItemFilter(factory.getKits().parseRequiredItem(el));
  }

  @MethodParser("holding")
  public HoldingItemFilter parseHolding(Element el) throws InvalidXMLException {
    return new HoldingItemFilter(factory.getKits().parseRequiredItem(el));
  }

  @MethodParser("wearing")
  public WearingItemFilter parseWearingItem(Element el) throws InvalidXMLException {
    return new WearingItemFilter(factory.getKits().parseRequiredItem(el));
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
      duration =
          Range.closed(
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
    final Duration duration = XMLUtils.parseDuration(Node.fromRequiredAttr(el, "duration"), null);
    if (!duration.isNegative() && !duration.isZero()) {
      return new MonostableFilter(parseChild(el), duration);
    } else {
      return StaticFilter.DENY;
    }
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
    return parseMatchPhaseFilter(el.getValue(), el);
  }

  @MethodParser("match-started")
  public Filter parseMatchStarted(Element el) throws InvalidXMLException {
    return parseMatchPhaseFilter("started", el);
  }

  @MethodParser("match-running")
  public Filter parseMatchRunning(Element el) throws InvalidXMLException {
    return parseMatchPhaseFilter("running", el);
  }

  @MethodParser("match-finished")
  public Filter parseMatchFinished(Element el) throws InvalidXMLException {
    return parseMatchPhaseFilter("finished", el);
  }

  private Filter parseMatchPhaseFilter(String matchState, Element el) throws InvalidXMLException {

    switch (matchState) {
      case "running":
        return MatchPhaseFilter.RUNNING;
      case "finished":
        return MatchPhaseFilter.FINISHED;
      case "starting":
        return MatchPhaseFilter.STARTING;
      case "idle":
        return MatchPhaseFilter.IDLE;
      case "started":
        return MatchPhaseFilter.STARTED;
    }

    throw new InvalidXMLException("Invalid or no match state found", el);
  }

  // Methods for parsing QueryModifiers

  @MethodParser("offset")
  public LocationQueryModifier parseOffsetFilter(Element el) throws InvalidXMLException {
    String value = el.getAttributeValue("vector");
    if (value == null) throw new InvalidXMLException("No vector provided", el);
    // Check vector format
    Vector vector = XMLUtils.parseVector(new Node(el), value.replaceAll("[\\^~]", ""));

    String[] coords = value.split("\\s*,\\s*");

    boolean[] relative = new boolean[3];

    Boolean local = null;
    for (int i = 0; i < coords.length; i++) {
      String coord = coords[i];

      if (local == null) {
        local = coord.startsWith("^");
      }

      if (coord.startsWith("^") != local)
        throw new InvalidXMLException("Cannot mix world & local coordinates", el);

      relative[i] = coord.startsWith("~");
    }

    if (local == null) throw new InvalidXMLException("No coordinates provided", el);

    if (local) {
      return new LocationQueryModifier.Local(parseChild(el), vector);
    } else {
      return new LocationQueryModifier.World(parseChild(el), vector, relative);
    }
  }

  @MethodParser("player")
  public PlayerBlockQueryModifier parsePlayerFilter(Element el) throws InvalidXMLException {
    return new PlayerBlockQueryModifier(parseChild(el));
  }

  @MethodParser("players")
  public PlayerCountFilter parsePlayerCountFilter(Element el) throws InvalidXMLException {
    Filter child =
        el.getChildren().isEmpty()
            ? parseFilterProperty(el, "filter", StaticFilter.ALLOW)
            : parseChild(el);

    return new PlayerCountFilter(
        child,
        XMLUtils.parseNumericRange(el, Integer.class, Range.atLeast(1)),
        XMLUtils.parseBoolean(el.getAttribute("participants"), true),
        XMLUtils.parseBoolean(el.getAttribute("observers"), false));
  }
}
