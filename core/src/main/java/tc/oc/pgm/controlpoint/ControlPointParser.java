package tc.oc.pgm.controlpoint;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.block.BlockFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.kits.tag.ItemModifier;
import tc.oc.pgm.payload.PayloadDefinition;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class ControlPointParser {
  private static final Filter VISUAL_MATERIALS =
      AnyFilter.of(
          ItemModifier.COLOR_AFFECTED.stream().map(BlockFilter::new).collect(Collectors.toList()));

  public enum Type {
    HILL,
    POINT,
    PAYLOAD
  }

  public static ControlPointDefinition parseControlPoint(
      MapFactory factory, Element el, Type type, AtomicInteger serialNumber)
      throws InvalidXMLException {
    String id = el.getAttributeValue("id");
    RegionParser regionParser = factory.getRegions();
    FilterParser filterParser = factory.getFilters();

    Region captureRegion =
        type == Type.PAYLOAD
            ? EverywhereRegion.INSTANCE
            : regionParser.parseProperty(
                Node.fromRequiredChildOrAttr(el, "capture-region", "capture"));
    Region progressDisplayRegion =
        regionParser.parseProperty(
            Node.fromChildOrAttr(el, "progress-display-region", "progress"),
            BlockBoundedValidation.INSTANCE);
    Region ownerDisplayRegion =
        regionParser.parseProperty(
            Node.fromChildOrAttr(el, "owner-display-region", "captured"),
            BlockBoundedValidation.INSTANCE);

    Filter captureFilter = filterParser.parseFilterProperty(el, "capture-filter");
    Filter playerFilter = filterParser.parseFilterProperty(el, "player-filter");

    Filter visualMaterials;
    List<Filter> filters = filterParser.parseFiltersProperty(el, "visual-materials");
    if (filters.isEmpty()) {
      visualMaterials = VISUAL_MATERIALS;
    } else {
      visualMaterials = AnyFilter.of(filters);
    }

    String name;
    Attribute attrName = el.getAttribute("name");

    if (attrName != null) {
      name = attrName.getValue();
    } else {
      int serial = serialNumber.getAndIncrement();
      name = "Hill";
      if (serial > 1) name += " " + serial;
    }

    TeamModule teams = factory.getModule(TeamModule.class);
    TeamFactory initialOwner =
        teams == null ? null : teams.parseTeam(el.getAttribute("initial-owner"), factory);
    Vector capturableDisplayBeacon = XMLUtils.parseVector(el.getAttribute("beacon"));
    Duration timeToCapture =
        XMLUtils.parseDuration(el.getAttribute("capture-time"), Duration.ofSeconds(30));

    final double decayRate, recoveryRate, ownedDecayRate, contestedRate;
    final Node attrIncremental = Node.fromAttr(el, "incremental");
    final Node attrDecay = Node.fromAttr(el, "decay", "decay-rate");
    final Node attrRecovery = Node.fromAttr(el, "recovery", "recovery-rate");
    final Node attrOwnedDecay = Node.fromAttr(el, "owned-decay", "owned-decay-rate");
    final Node attrContested = Node.fromAttr(el, "contested", "contested-rate");
    boolean koth = type == Type.HILL;
    boolean pd = type == Type.PAYLOAD;

    if (attrIncremental == null) {
      recoveryRate =
          XMLUtils.parseNumber(
              attrRecovery, Double.class, koth || pd ? 1D : Double.POSITIVE_INFINITY);
      decayRate =
          XMLUtils.parseNumber(
              attrDecay, Double.class, koth || pd ? 0.0 : Double.POSITIVE_INFINITY);
      ownedDecayRate = XMLUtils.parseNumber(attrOwnedDecay, Double.class, 0.0);
    } else {
      if (attrDecay != null || attrRecovery != null || attrOwnedDecay != null)
        throw new InvalidXMLException(
            "Cannot combine this attribute with incremental",
            attrDecay != null ? attrDecay : attrRecovery != null ? attrRecovery : attrOwnedDecay);

      final boolean incremental = XMLUtils.parseBoolean(attrIncremental, koth || pd);
      recoveryRate = incremental ? 1.0 : Double.POSITIVE_INFINITY;
      decayRate = incremental ? 0.0 : Double.POSITIVE_INFINITY;
      ownedDecayRate = 0.0;
    }
    contestedRate = XMLUtils.parseNumber(attrContested, Double.class, decayRate);

    float timeMultiplier =
        XMLUtils.parseNumber(el.getAttribute("time-multiplier"), Float.class, koth ? 0.1f : 0f);
    boolean neutralState = XMLUtils.parseBoolean(el.getAttribute("neutral-state"), koth || pd);

    if (!neutralState && ownedDecayRate > 0) {
      throw new InvalidXMLException("This attribute requires a neutral state.", attrOwnedDecay);
    }
    boolean permanent = XMLUtils.parseBoolean(el.getAttribute("permanent"), false);
    float pointsPerSecond =
        XMLUtils.parseNumber(el.getAttribute("points"), Float.class, pd ? 0f : 1f);
    float pointsOwner =
        XMLUtils.parseNumber(el.getAttribute("owner-points"), Float.class, pd ? 1f : 0f);
    float pointsGrowth =
        XMLUtils.parseNumber(
            el.getAttribute("points-growth"), Float.class, Float.POSITIVE_INFINITY);
    boolean showProgress = XMLUtils.parseBoolean(el.getAttribute("show-progress"), koth || pd);
    ShowOptions options = ShowOptions.parse(filterParser, el);
    Boolean required = XMLUtils.parseBoolean(el.getAttribute("required"), null);

    ControlPointDefinition.CaptureCondition captureCondition =
        XMLUtils.parseEnum(
            Node.fromAttr(el, "capture-rule"),
            ControlPointDefinition.CaptureCondition.class,
            "capture rule",
            ControlPointDefinition.CaptureCondition.EXCLUSIVE);

    if (pd) {
      BlockVector location =
          BlockVectors.center(XMLUtils.parseVector(Node.fromRequiredAttr(el, "location")));
      double radius = XMLUtils.parseNumber(Node.fromRequiredAttr(el, "radius"), Double.class);
      Filter displayFilter =
          filterParser.parseFilterProperty(el, "display-filter", StaticFilter.ALLOW);
      return new PayloadDefinition(
          id,
          name,
          required,
          options,
          captureRegion,
          captureFilter,
          playerFilter,
          progressDisplayRegion,
          ownerDisplayRegion,
          visualMaterials,
          capturableDisplayBeacon == null ? null : capturableDisplayBeacon.toBlockVector(),
          timeToCapture,
          decayRate,
          recoveryRate,
          ownedDecayRate,
          contestedRate,
          timeMultiplier,
          initialOwner,
          captureCondition,
          neutralState,
          permanent,
          pointsPerSecond,
          pointsOwner,
          pointsGrowth,
          showProgress,
          location,
          radius,
          displayFilter);
    }

    return new ControlPointDefinition(
        id,
        name,
        required,
        options,
        captureRegion,
        captureFilter,
        playerFilter,
        progressDisplayRegion,
        ownerDisplayRegion,
        visualMaterials,
        capturableDisplayBeacon == null ? null : capturableDisplayBeacon.toBlockVector(),
        timeToCapture,
        decayRate,
        recoveryRate,
        ownedDecayRate,
        contestedRate,
        timeMultiplier,
        initialOwner,
        captureCondition,
        neutralState,
        permanent,
        pointsPerSecond,
        pointsOwner,
        pointsGrowth,
        showProgress);
  }
}
