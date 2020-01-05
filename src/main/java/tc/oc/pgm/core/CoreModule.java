package tc.oc.pgm.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jdom2.Attribute;
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
import tc.oc.pgm.modules.InfoModule;
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

@ModuleDescription(
    name = "Cores",
    depends = {RegionModule.class, TeamModule.class, GoalModule.class, InfoModule.class})
public class CoreModule extends MapModule<CoreMatchModule> {

  private static final MapTag CORE_TAG = MapTag.forName("core");

  protected final List<CoreFactory> coreFactories;

  public CoreModule(List<CoreFactory> coreFactories) {
    assert coreFactories.size() > 0;
    this.coreFactories = coreFactories;
  }

  @Override
  public void loadTags(Set<MapTag> tags) {
    tags.add(CORE_TAG);
  }

  @Override
  public CoreMatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Core> cores = new ImmutableList.Builder<>();
    for (CoreFactory factory : this.coreFactories) {
      Core core = new Core(factory, match);
      match.getFeatureContext().add(core);
      match.needMatchModule(GoalMatchModule.class).addGoal(core);
      cores.add(core);
    }

    return new CoreMatchModule(match, cores.build());
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static CoreModule parse(MapModuleContext context, Logger logger, Document doc)
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
      RegionParser parser = context.getRegionParser();
      if (context
          .getModule(InfoModule.class)
          .getMapInfo()
          .proto
          .isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
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
        // If there are already nameless cores for this team, name this one "Core #", starting at 2
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
      context.features().addFeature(coreEl, factory);
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
