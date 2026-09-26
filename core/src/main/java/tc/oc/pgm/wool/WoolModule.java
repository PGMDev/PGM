package tc.oc.pgm.wool;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.DyeColor;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class WoolModule implements MapModule<WoolMatchModule> {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("wool", Gamemode.CAPTURE_THE_WOOL, false));

  protected final Multimap<TeamFactory, MonumentWoolFactory> woolFactories;

  public WoolModule(Multimap<TeamFactory, MonumentWoolFactory> woolFactories) {
    assert !woolFactories.isEmpty();
    this.woolFactories = woolFactories;
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  public Multimap<TeamFactory, MonumentWoolFactory> getWools() {
    return woolFactories;
  }

  @Override
  public WoolMatchModule createMatchModule(Match match) {
    Multimap<Team, MonumentWool> wools = ArrayListMultimap.create();
    for (Entry<TeamFactory, MonumentWoolFactory> woolEntry : this.woolFactories.entries()) {
      Team team = match.needModule(TeamMatchModule.class).getTeam(woolEntry.getKey());
      MonumentWool wool = new MonumentWool(woolEntry.getValue(), match);
      match.getFeatureContext().add(wool);
      wools.put(team, wool);
      match.needModule(GoalMatchModule.class).addGoal(wool);
    }
    return new WoolMatchModule(match, wools);
  }

  public static class Factory implements MapModuleFactory<WoolModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, TeamModule.class);
    }

    @Override
    public WoolModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Multimap<TeamFactory, MonumentWoolFactory> woolFactories = ArrayListMultimap.create();
      var parser = factory.getParser();

      for (Element woolEl : XMLUtils.flattenElements(doc.getRootElement(), "wools", "wool")) {
        String id = parser.string(woolEl, "id").orNull();
        boolean craftable = parser.parseBool(woolEl, "craftable").orTrue();
        TeamFactory team =
            parser.node(n -> Teams.getTeam(n, factory), woolEl, "team").required();
        DyeColor color = parser.node(XMLUtils::parseDyeColor, woolEl, "color").required();
        Region placement = parser.region(woolEl, "monument").legacy(factory).required();
        ShowOptions options = ShowOptions.parse(factory.getFilters(), woolEl);
        Boolean required = parser.parseBool(woolEl, "required").orNull();

        ProximityMetric woolProximityMetric = ProximityMetric.parse(
            woolEl, "wool", new ProximityMetric(ProximityMetric.Type.CLOSEST_KILL, false));
        ProximityMetric monumentProximityMetric = ProximityMetric.parse(
            woolEl, "monument", new ProximityMetric(ProximityMetric.Type.CLOSEST_BLOCK, false));

        Vector location;
        if (factory.getProto().isOlderThan(MapProtos.WOOL_LOCATIONS)) {
          // The default location is at infinity, so players/blocks are always an infinite distance
          // from it
          location = new Vector(
              Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        } else {
          location = parser.vector(woolEl, "location").required();
        }

        MonumentWoolFactory wool = new MonumentWoolFactory(
            id,
            required,
            options,
            team,
            woolProximityMetric,
            monumentProximityMetric,
            color,
            location,
            placement,
            craftable);
        factory.getFeatures().addFeature(woolEl, wool);
        woolFactories.put(team, wool);
      }

      return woolFactories.isEmpty() ? null : new WoolModule(woolFactories);
    }
  }
}
