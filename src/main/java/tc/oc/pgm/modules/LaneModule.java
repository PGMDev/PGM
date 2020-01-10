package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class LaneModule implements MapModule {
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

  public static class Factory implements MapModuleFactory<LaneModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, TeamModule.class);
    }

    @Override
    public LaneModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<TeamFactory, Region> lanes = Maps.newHashMap();
      TeamModule teamModule = context.getModule(TeamModule.class);

      for (Element laneEl : XMLUtils.flattenElements(doc.getRootElement(), "lanes", "lane")) {
        TeamFactory team =
            teamModule.parseTeam(XMLUtils.getRequiredAttribute(laneEl, "team"), context);
        Region region = context.legacy().getRegions().parseChildren(laneEl);
        lanes.put(team, region);
      }

      if (lanes.size() > 0) {
        return new LaneModule(lanes);
      } else {
        return null;
      }
    }
  }
}
