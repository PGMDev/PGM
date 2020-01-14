package tc.oc.pgm.controlpoint;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.AnyFilter;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

import java.util.List;

public abstract class ControlPointParser {
  private static final Filter VISUAL_MATERIALS =
      AnyFilter.of(
          new BlockFilter(Material.WOOL),
          new BlockFilter(Material.CARPET),
          new BlockFilter(Material.STAINED_CLAY),
          new BlockFilter(Material.STAINED_GLASS),
          new BlockFilter(Material.STAINED_GLASS_PANE));

  public static ControlPointDefinition parseControlPoint(
          MapFactory factory, Element elControlPoint, boolean koth) throws InvalidXMLException {
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
    List<Filter> filters = filterParser.parseFiltersProperty(elControlPoint, "visual-world");
    if (filters.isEmpty()) {
      visualMaterials = VISUAL_MATERIALS;
    } else {
      visualMaterials = new AnyFilter(filters);
    }

    String name = elControlPoint.getAttributeValue("name", "Hill");
    TeamFactory initialOwner =
            factory
            .getModule(TeamModule.class)
            .parseTeam(elControlPoint.getAttribute("initial-owner"), factory);
    Vector capturableDisplayBeacon = XMLUtils.parseVector(elControlPoint.getAttribute("beacon"));
    Duration timeToCapture =
        XMLUtils.parseDuration(
            elControlPoint.getAttribute("capture-time"), Duration.standardSeconds(30));

    float timeMultiplier =
        XMLUtils.parseNumber(
            elControlPoint.getAttribute("time-multiplier"), Float.class, koth ? 0.1f : 0f);
    boolean incrementalCapture =
        XMLUtils.parseBoolean(elControlPoint.getAttribute("incremental"), koth);
    boolean neutralState =
        XMLUtils.parseBoolean(elControlPoint.getAttribute("neutral-state"), koth);
    boolean permanent = XMLUtils.parseBoolean(elControlPoint.getAttribute("permanent"), false);
    float pointsPerSecond =
        XMLUtils.parseNumber(elControlPoint.getAttribute("points"), Float.class, 1f);
    float pointsGrowth =
        XMLUtils.parseNumber(
            elControlPoint.getAttribute("points-growth"), Float.class, Float.POSITIVE_INFINITY);
    boolean showProgress =
        XMLUtils.parseBoolean(elControlPoint.getAttribute("show-progress"), koth);
    boolean visible = XMLUtils.parseBoolean(elControlPoint.getAttribute("show"), true);
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
        visible,
        captureRegion,
        captureFilter,
        playerFilter,
        progressDisplayRegion,
        ownerDisplayRegion,
        visualMaterials,
        capturableDisplayBeacon == null ? null : capturableDisplayBeacon.toBlockVector(),
        timeToCapture,
        timeMultiplier,
        initialOwner,
        captureCondition,
        incrementalCapture,
        neutralState,
        permanent,
        pointsPerSecond,
        pointsGrowth,
        showProgress);
  }
}
