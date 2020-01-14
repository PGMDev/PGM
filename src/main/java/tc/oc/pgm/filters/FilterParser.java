package tc.oc.pgm.filters;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.PlayerRelation;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.classes.PlayerClass;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.flag.Post;
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
import tc.oc.pgm.util.XMLUtils;
import tc.oc.util.StringUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    return new InverseFilter(new AnyFilter(parseChildren(el)));
  }

  @MethodParser("team")
  public TeamFilter parseTeam(Element el) throws InvalidXMLException {
    return new TeamFilter(Teams.getTeamRef(new Node(el), this.factory));
  }

  @MethodParser("same-team")
  public SameTeamFilter parseSameTeam(Element el) throws InvalidXMLException {
    return new SameTeamFilter(parseChild(el));
  }

  @MethodParser("attacker")
  public AttackerFilter parseAttacker(Element el) throws InvalidXMLException {
    return new AttackerFilter(parseChild(el));
  }

  @MethodParser("victim")
  public VictimFilter parseVictim(Element el) throws InvalidXMLException {
    return new VictimFilter(parseChild(el));
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

    return new KillStreakFilter(range, repeat);
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

  @MethodParser("objective")
  public GoalFilter parseGoal(Element el) throws InvalidXMLException {
    XMLFeatureReference<? extends GoalDefinition> goal =
        this.factory.getFeatures().createReference(new Node(el), GoalDefinition.class);
    boolean anyTeam = XMLUtils.parseBoolean(el.getAttribute("any"), false);

    Attribute attrTeam = el.getAttribute("team");
    XMLFeatureReference<TeamFactory> team;
    if (attrTeam != null) {
      if (anyTeam) throw new InvalidXMLException("Cannot combine attributes 'team' and 'any'", el);
      team =
          this.factory

              .getFeatures()
              .createReference(new Node(attrTeam), TeamFactory.class);
    } else {
      team = null;
    }

    return new GoalFilter(goal, team, anyTeam);
  }

  protected FlagStateFilter parseFlagState(Element el, Class<? extends State> state)
      throws InvalidXMLException {
    Node postAttr = Node.fromAttr(el, "post");
    return new FlagStateFilter(
        this.factory.getFeatures().createReference(new Node(el), FlagDefinition.class),
        postAttr == null
            ? null
            : this.factory.getFeatures().createReference(postAttr, Post.class),
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

  @MethodParser("structural-load")
  public StructuralLoadFilter parseStructuralLoad(Element el) throws InvalidXMLException {
    return new StructuralLoadFilter(XMLUtils.parseNumber(el, Integer.class));
  }

  @MethodParser("time")
  public TimeFilter parseTimeFilter(Element el) throws InvalidXMLException {
    return new TimeFilter(XMLUtils.parseDuration(el, null));
  }
}
