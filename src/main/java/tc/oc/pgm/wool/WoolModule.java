package tc.oc.pgm.wool;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.DyeColor;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.GoalModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Wool",
    depends = {RegionModule.class, TeamModule.class, GoalModule.class})
public class WoolModule extends MapModule<WoolMatchModule> {

  private static final MapTag WOOL_TAG = MapTag.forName("wool");

  protected final Multimap<TeamFactory, MonumentWoolFactory> woolFactories;

  public WoolModule(Multimap<TeamFactory, MonumentWoolFactory> woolFactories) {
    assert woolFactories.size() > 0;
    this.woolFactories = woolFactories;
  }

  public Multimap<TeamFactory, MonumentWoolFactory> getWools() {
    return woolFactories;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void loadTags(Set tags) {
    tags.add(WOOL_TAG);
  }

  @Override
  public WoolMatchModule createMatchModule(Match match) {
    Multimap<Team, MonumentWool> wools = ArrayListMultimap.create();
    for (Entry<TeamFactory, MonumentWoolFactory> woolEntry : this.woolFactories.entries()) {
      Team team = match.needMatchModule(TeamMatchModule.class).getTeam(woolEntry.getKey());
      MonumentWool wool = new MonumentWool(woolEntry.getValue(), match);
      match.getFeatureContext().add(wool);
      wools.put(team, wool);
      match.needMatchModule(GoalMatchModule.class).addGoal(wool);
    }
    return new WoolMatchModule(match, wools);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static WoolModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Multimap<TeamFactory, MonumentWoolFactory> woolFactories = ArrayListMultimap.create();
    TeamModule teamModule = context.getModule(TeamModule.class);
    RegionParser parser = context.getRegionParser();

    for (Element woolEl : XMLUtils.flattenElements(doc.getRootElement(), "wools", "wool")) {
      String id = woolEl.getAttributeValue("id");
      boolean craftable = Boolean.parseBoolean(woolEl.getAttributeValue("craftable", "true"));
      TeamFactory team =
          teamModule.parseTeam(XMLUtils.getRequiredAttribute(woolEl, "team"), context);
      DyeColor color = XMLUtils.parseDyeColor(XMLUtils.getRequiredAttribute(woolEl, "color"));
      Region placement;
      if (context.getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
        placement = parser.parseChildren(woolEl);
      } else {
        placement = parser.parseRequiredRegionProperty(woolEl, "monument");
      }
      boolean visible = XMLUtils.parseBoolean(woolEl.getAttribute("show"), true);
      Boolean required = XMLUtils.parseBoolean(woolEl.getAttribute("required"), null);

      ProximityMetric woolProximityMetric =
          ProximityMetric.parse(
              woolEl, "wool", new ProximityMetric(ProximityMetric.Type.CLOSEST_KILL, false));
      ProximityMetric monumentProximityMetric =
          ProximityMetric.parse(
              woolEl, "monument", new ProximityMetric(ProximityMetric.Type.CLOSEST_BLOCK, false));

      Vector location;
      if (context.getProto().isOlderThan(ProtoVersions.WOOL_LOCATIONS)) {
        // The default location is at infinity, so players/blocks are always an infinite distance
        // from it
        location =
            new Vector(
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      } else {
        location = XMLUtils.parseVector(XMLUtils.getRequiredAttribute(woolEl, "location"));
      }

      MonumentWoolFactory wool =
          new MonumentWoolFactory(
              id,
              required,
              visible,
              team,
              woolProximityMetric,
              monumentProximityMetric,
              color,
              location,
              placement,
              craftable);
      context.features().addFeature(woolEl, wool);
      woolFactories.put(team, wool);
    }

    if (woolFactories.size() > 0) {
      return new WoolModule(woolFactories);
    } else {
      return null;
    }
  }
}
