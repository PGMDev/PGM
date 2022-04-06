package tc.oc.pgm.filters;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.MethodParser;
import tc.oc.pgm.api.StringUtils;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterParser;
import tc.oc.pgm.api.goal.GoalDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;
import tc.oc.pgm.api.xml.XMLFeatureReference;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.filters.modifier.PlayerBlockQueryModifier;
import tc.oc.pgm.filters.modifier.location.LocalLocationQueryModifier;
import tc.oc.pgm.filters.modifier.location.LocationQueryModifier;
import tc.oc.pgm.filters.modifier.location.WorldLocationQueryModifier;
import tc.oc.pgm.filters.modifier.relation.AttackerQueryModifier;
import tc.oc.pgm.filters.modifier.relation.SameTeamQueryModifier;
import tc.oc.pgm.filters.modifier.relation.VictimQueryModifier;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.flag.state.Captured;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.flag.state.Dropped;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.flag.state.State;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class FilterParserImpl implements FilterParser {

  protected final Map<String, Method> methodParsers;
  protected final MapFactory factory;
  protected final TeamModule teamModule;

  public FilterParserImpl(MapFactory factory) {
    this.factory = factory;
    this.teamModule = factory.getModule(TeamModule.class);

    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  @Override
  public Filter parseReference(Node node) throws InvalidXMLException {
    return parseReference(node, node.getValue());
  }

  @Override
  public boolean isFilter(Element el) {
    return methodParsers.containsKey(el.getName()) || factory.getRegions().isRegion(el);
  }

  @Override
  public List<Element> getFilterChildren(Element parent) {
    List<Element> elements = new ArrayList<Element>();
    for (Element el : parent.getChildren()) {
      if (this.isFilter(el)) {
        elements.add(el);
      }
    }
    return elements;
  }

  @Override
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

  @Override
  public Filter parseChild(Element parent) throws InvalidXMLException {
    if (parent.getChildren().isEmpty()) {
      throw new InvalidXMLException("Expected a child filter", parent);
    } else if (parent.getChildren().size() > 1) {
      throw new InvalidXMLException("Expected only one child filter, not multiple", parent);
    }
    return this.parse(parent.getChildren().get(0));
  }

  @Override
  public List<Filter> parseChildren(Element parent) throws InvalidXMLException {
    List<Filter> filters = Lists.newArrayList();
    for (Element el : parent.getChildren()) {
      filters.add(this.parse(el));
    }
    return filters;
  }

  /** These "property" methods are the ones to use for parsing filters as part of other modules. */
  @Override
  public @Nullable Filter parseFilterProperty(Element el, String name) throws InvalidXMLException {
    return this.parseFilterProperty(el, name, null);
  }

  @Override
  public Filter parseRequiredFilterProperty(Element el, String name) throws InvalidXMLException {
    Filter filter = this.parseFilterProperty(el, name);
    if (filter == null) throw new InvalidXMLException("Missing required filter '" + name + "'", el);
    return filter;
  }

  @Override
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
  @Override
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
  @Override
  @MethodParser("always")
  public Filter parseAlways(Element el) {
    return StaticFilter.ALLOW;
  }

  @Override
  @MethodParser("never")
  public Filter parseNever(Element el) {
    return StaticFilter.DENY;
  }

  @Override
  @MethodParser("any")
  public Filter parseAny(Element el) throws InvalidXMLException {
    return new AnyFilter(parseChildren(el));
  }

  @Override
  @MethodParser("all")
  public Filter parseAll(Element el) throws InvalidXMLException {
    return new AllFilter(parseChildren(el));
  }

  @Override
  @MethodParser("one")
  public Filter parseOne(Element el) throws InvalidXMLException {
    return new OneFilter(parseChildren(el));
  }

  @Override
  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(new AnyFilter(parseChildren(el)));
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

  @Override
  @MethodParser("material")
  public Filter parseMaterial(Element el) throws InvalidXMLException {
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
    Integer count = XMLUtils.parseNumber(el.getAttribute("count"), Integer.class, (Integer) null);
    Integer min = XMLUtils.parseNumber(el.getAttribute("min"), Integer.class, (Integer) null);
    Integer max = XMLUtils.parseNumber(el.getAttribute("max"), Integer.class, (Integer) null);
    Range<Integer> range;

    if (count != null) {
      range = Range.singleton(count);
    } else if (min == null) {
      if (max == null) {
        throw new InvalidXMLException("kill-streak filter must have a count, min, or max", el);
      } else {
        range = Range.atMost(max);
      }
    } else {
      if (max == null) {
        range = Range.atLeast(min);
      } else {
        range = Range.closed(min, max);
      }
    }

    if (!range.hasUpperBound() && repeat) {
      throw new InvalidXMLException(
          "kill-streak filters with repeat=\"true\" must define a max or count", el);
    }

    return new KillStreakFilter(range, repeat);
  }

  @MethodParser("lives")
  public LivesFilter parseLives(Element el) throws InvalidXMLException {
    Integer count = XMLUtils.parseNumber(el.getAttribute("count"), Integer.class, (Integer) null);
    Integer min = XMLUtils.parseNumber(el.getAttribute("min"), Integer.class, (Integer) null);
    Integer max = XMLUtils.parseNumber(el.getAttribute("max"), Integer.class, (Integer) null);
    Range<Integer> range;

    if (count != null) {
      range = Range.singleton(count);
    } else if (min == null) {
      if (max == null) {
        throw new InvalidXMLException("lives filter must have a count, min, or max", el);
      } else {
        range = Range.atMost(max);
      }
    } else {
      if (max == null) {
        range = Range.atLeast(min);
      } else {
        range = Range.closed(min, max);
      }
    }

    return new LivesFilter(range);
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

  @MethodParser("objective")
  public GoalFilter parseGoal(Element el) throws InvalidXMLException {
    XMLFeatureReference<? extends GoalDefinition> goal =
        this.factory.getFeatures().createReference(new Node(el), GoalDefinition.class);
    boolean anyTeam = XMLUtils.parseBoolean(el.getAttribute("any"), false);

    Attribute attrTeam = el.getAttribute("team");
    XMLFeatureReference<TeamFactory> team;
    if (attrTeam != null) {
      if (anyTeam) throw new InvalidXMLException("Cannot combine attributes 'team' and 'any'", el);
      team = this.factory.getFeatures().createReference(new Node(attrTeam), TeamFactory.class);

    } else {
      team = null;
    }

    return new GoalFilter(goal, team, anyTeam);
  }

  @MethodParser("completed")
  public GoalFilter parseCompleted(Element el) throws InvalidXMLException {
    XMLFeatureReference<? extends GoalDefinition> goal =
        this.factory.getFeatures().createReference(new Node(el), GoalDefinition.class);

    return new GoalFilter(goal, null, true);
  }

  @MethodParser("captured")
  public GoalFilter parseCaptured(Element el) throws InvalidXMLException {
    XMLFeatureReference<? extends GoalDefinition> goal =
        this.factory.getFeatures().createReference(new Node(el), GoalDefinition.class);

    Attribute attrTeam = el.getAttribute("team");
    XMLFeatureReference<TeamFactory> team =
        attrTeam != null
            ? this.factory.getFeatures().createReference(new Node(attrTeam), TeamFactory.class)
            : null;

    return new GoalFilter(goal, team, false);
  }

  protected FlagStateFilter parseFlagState(Element el, Class<? extends State> state)
      throws InvalidXMLException {
    Node postAttr = Node.fromAttr(el, "post");
    return new FlagStateFilter(
        this.factory.getFeatures().createReference(new Node(el), FlagDefinition.class),
        postAttr == null ? null : this.factory.getFeatures().createReference(postAttr, Post.class),
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
  public TimeFilter parseTimeFilter(Element el) throws InvalidXMLException {
    return new TimeFilter(XMLUtils.parseDuration(el, null));
  }

  @MethodParser("score")
  public ScoreFilter parseScoreFilter(Element el) throws InvalidXMLException {
    return new ScoreFilter(XMLUtils.parseNumericRange(new Node(el), Integer.class));
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
      return new LocalLocationQueryModifier(parseChild(el), vector);
    } else {
      return new WorldLocationQueryModifier(parseChild(el), vector, relative);
    }
  }

  @MethodParser("player")
  public PlayerBlockQueryModifier parsePlayerFilter(Element el) throws InvalidXMLException {
    return new PlayerBlockQueryModifier(parseChild(el));
  }
}
