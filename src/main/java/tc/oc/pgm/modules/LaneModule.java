package tc.oc.pgm.modules;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Void Lane",
    depends = {RegionModule.class, TeamModule.class})
public class LaneModule extends MapModule {
  private final Map<TeamFactory, Region> lanes;

  public LaneModule(Map<TeamFactory, Region> lanes) {
    this.lanes = lanes;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    Map<Team, Region> lanes = Maps.newHashMapWithExpectedSize(this.lanes.size());
    for (Entry<TeamFactory, Region> entry : this.lanes.entrySet()) {
      lanes.put(
          match.needMatchModule(TeamMatchModule.class).getTeam(entry.getKey()), entry.getValue());
    }
    return new LaneMatchModule(match, lanes);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static LaneModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Map<TeamFactory, Region> lanes = Maps.newHashMap();
    TeamModule teamModule = context.getModule(TeamModule.class);

    for (Element laneEl : XMLUtils.flattenElements(doc.getRootElement(), "lanes", "lane")) {
      TeamFactory team =
          teamModule.parseTeam(XMLUtils.getRequiredAttribute(laneEl, "team"), context);
      Region region = context.getRegionParser().parseChildren(laneEl);
      lanes.put(team, region);
    }

    if (lanes.size() > 0) {
      return new LaneModule(lanes);
    } else {
      return null;
    }
  }
}
