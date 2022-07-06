package tc.oc.pgm.controlpoint;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.AnyFilter;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public abstract class ControlPointParser {
  private static final Filter VISUAL_MATERIALS =
      AnyFilter.of(
          new BlockFilter(Material.WOOL),
          new BlockFilter(Material.CARPET),
          new BlockFilter(Material.STAINED_CLAY),
          new BlockFilter(Material.STAINED_GLASS),
          new BlockFilter(Material.STAINED_GLASS_PANE));

  public static ControlPointDefinition parseControlPoint(
      MapFactory factory, Element elControlPoint, boolean koth, AtomicInteger serialNumber)
      throws InvalidXMLException {
    String id = elControlPoint.getAttributeValue("id");
    RegionParser regionParser = factory.getRegions();
    FilterParser filterParser = factory.getFilters();

    Region captureRegion =
        regionParser.parseRequiredRegionProperty(elControlPoint, "capture-region", "capture");
    Region progressDisplayRegion =
        regionParser.parseRegionProperty(
            elControlPoint, BlockBoundedValidation.INSTANCE, "progress-display-region", "progress");
    Region ownerDisplayRegion =
        regionParser.parseRegionProperty(
            elControlPoint, BlockBoundedValidation.INSTANCE, "owner-display-region", "captured");

    Filter captureFilter = filterParser.parseFilterProperty(elControlPoint, "capture-filter");
    Filter playerFilter = filterParser.parseFilterProperty(elControlPoint, "player-filter");

    Filter visualMaterials;
    List<Filter> filters = filterParser.parseFiltersProperty(elControlPoint, "visual-materials");
    if (filters.isEmpty()) {
      visualMaterials = VISUAL_MATERIALS;
    } else {
      visualMaterials = new AnyFilter(filters);
    }

    String name;
    Attribute attrName = elControlPoint.getAttribute("name");

    if (attrName != null) {
      name = attrName.getValue();
    } else {
      int serial = serialNumber.getAndIncrement();
      name = "Hill";
      if (serial > 1) name += " " + serial;
    }

    TeamModule teams = factory.getModule(TeamModule.class);
    TeamFactory initialOwner =
        teams == null
            ? null
            : teams.parseTeam(elControlPoint.getAttribute("initial-owner"), factory);
    Vector capturableDisplayBeacon = XMLUtils.parseVector(elControlPoint.getAttribute("beacon"));
    Duration timeToCapture =
        XMLUtils.parseDuration(elControlPoint.getAttribute("capture-time"), Duration.ofSeconds(30));

    final double decayRate, recoveryRate, ownedDecayRate, contestedRate;
    final Node attrIncremental = Node.fromAttr(elControlPoint, "incremental");
    final Node attrDecay = Node.fromAttr(elControlPoint, "decay", "decay-rate");
    final Node attrRecovery = Node.fromAttr(elControlPoint, "recovery", "recovery-rate");
    final Node attrOwnedDecay = Node.fromAttr(elControlPoint, "owned-decay", "owned-decay-rate");
    if (attrIncremental == null) {
      recoveryRate =
          XMLUtils.parseNumber(attrRecovery, Double.class, koth ? 1D : Double.POSITIVE_INFINITY);
      decayRate =
          XMLUtils.parseNumber(attrDecay, Double.class, koth ? 0.0 : Double.POSITIVE_INFINITY);
      ownedDecayRate = XMLUtils.parseNumber(attrOwnedDecay, Double.class, 0.0);
    } else {
      if (attrDecay != null || attrRecovery != null || attrOwnedDecay != null)
        throw new InvalidXMLException(
            "Cannot combine this attribute with incremental",
            attrDecay != null ? attrDecay : attrRecovery != null ? attrRecovery : attrOwnedDecay);

      final boolean incremental = XMLUtils.parseBoolean(attrIncremental, koth);
      recoveryRate = incremental ? 1.0 : Double.POSITIVE_INFINITY;
      decayRate = incremental ? 0.0 : Double.POSITIVE_INFINITY;
      ownedDecayRate = 0.0;
    }

    contestedRate =
        XMLUtils.parseNumber(
            Node.fromAttr(elControlPoint, "contested", "contested-rate"), Double.class, decayRate);

    float timeMultiplier =
        XMLUtils.parseNumber(
            elControlPoint.getAttribute("time-multiplier"), Float.class, koth ? 0.1f : 0f);
    boolean neutralState =
        XMLUtils.parseBoolean(elControlPoint.getAttribute("neutral-state"), koth);

    if (neutralState == false && ownedDecayRate > 0) {
      throw new InvalidXMLException("This attribute requires a neutral state.", attrOwnedDecay);
    }
    boolean permanent = XMLUtils.parseBoolean(elControlPoint.getAttribute("permanent"), false);
    float pointsPerSecond =
        XMLUtils.parseNumber(elControlPoint.getAttribute("points"), Float.class, 1f);
    float pointsOwner =
        XMLUtils.parseNumber(elControlPoint.getAttribute("owner-points"), Float.class, 0f);
    float pointsGrowth =
        XMLUtils.parseNumber(
            elControlPoint.getAttribute("points-growth"), Float.class, Float.POSITIVE_INFINITY);
    boolean showProgress =
        XMLUtils.parseBoolean(elControlPoint.getAttribute("show-progress"), koth);
    ShowOptions options = ShowOptions.parse(elControlPoint);
    Boolean required = XMLUtils.parseBoolean(elControlPoint.getAttribute("required"), null);

    ControlPointDefinition.CaptureCondition captureCondition =
        XMLUtils.parseEnum(
            Node.fromAttr(elControlPoint, "capture-rule"),
            ControlPointDefinition.CaptureCondition.class,
            "capture rule",
            ControlPointDefinition.CaptureCondition.EXCLUSIVE);

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
