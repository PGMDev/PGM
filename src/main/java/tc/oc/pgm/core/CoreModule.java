package tc.oc.pgm.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapInfoExtra;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class CoreModule implements MapModule, MapInfoExtra {
  protected final List<CoreFactory> coreFactories;

  public CoreModule(List<CoreFactory> coreFactories) {
    assert coreFactories.size() > 0;
    this.coreFactories = coreFactories;
  }

  @Override
  public Collection<Class> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public String getGenre() {
    return "Leak the Core";
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Core> cores = new ImmutableList.Builder<>();
    for (CoreFactory factory : this.coreFactories) {
      Core core = new Core(factory, match);
      match.getFeatureContext().add(core);
      match.needModule(GoalMatchModule.class).addGoal(core);
      cores.add(core);
    }

    return new CoreMatchModule(match, cores.build());
  }

  public static class Factory implements MapModuleFactory<CoreModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class, TeamModule.class);
    }

    @Override
    public CoreModule parse(MapFactory context, Logger logger, Document doc)
        throws InvalidXMLException {
      List<CoreFactory> coreFactories = Lists.newArrayList();
      HashMap<TeamFactory, Integer> serialNumbers = new HashMap<>();

      for (Element coreEl : XMLUtils.flattenElements(doc.getRootElement(), "cores", "core")) {
        MaterialData material =
            XMLUtils.parseBlockMaterialData(
                Node.fromAttr(coreEl, "material"), Material.OBSIDIAN.getNewData((byte) 0));

        int leakLevel = Integer.parseInt(coreEl.getAttributeValue("leak", "5"));

        // TODO: rename to owner on the next breaking revision
        TeamFactory owner =
            Teams.getTeam(XMLUtils.getRequiredAttribute(coreEl, "team").getValue(), context);
        Region region;
        RegionParser parser = context.getRegions();
        if (context.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
          region = parser.parseChildren(coreEl);
          parser.validate(region, BlockBoundedValidation.INSTANCE, new Node(coreEl));
        } else {
          region =
              parser.parseRequiredRegionProperty(coreEl, BlockBoundedValidation.INSTANCE, "region");
        }

        String id = coreEl.getAttributeValue("id");
        String name;

        Attribute attrName = coreEl.getAttribute("name");
        if (attrName != null) {
          name = attrName.getValue();
        } else if (!serialNumbers.containsKey(owner)) {
          // If this is the first nameless core for this team, name it "Core"
          name = "Core";
          serialNumbers.put(owner, 2);
        } else {
          // If there are already nameless cores for this team, name this one "Core #", starting at
          // 2
          int serial = serialNumbers.get(owner);
          name = "Core " + serial;
          serialNumbers.put(owner, serial + 1);
        }

        boolean modeChanges = XMLUtils.parseBoolean(coreEl.getAttribute("mode-changes"), false);
        boolean visible = XMLUtils.parseBoolean(coreEl.getAttribute("show"), true);
        Boolean required = XMLUtils.parseBoolean(coreEl.getAttribute("required"), null);
        ProximityMetric proximityMetric =
            ProximityMetric.parse(
                coreEl, new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));

        CoreFactory factory =
            new CoreFactory(
                id,
                name,
                required,
                visible,
                owner,
                proximityMetric,
                region,
                material,
                leakLevel,
                modeChanges);
        context.getFeatures().addFeature(coreEl, factory);
        coreFactories.add(factory);
      }

      // only produce a valid core module if there are cores to handle
      if (coreFactories.size() > 0) {
        return new CoreModule(coreFactories);
      } else {
        return null;
      }
    }
  }
}
