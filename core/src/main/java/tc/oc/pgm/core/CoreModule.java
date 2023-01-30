package tc.oc.pgm.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class CoreModule implements MapModule<CoreMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("dtc", "core", "Destroy the Core", true, false));
  protected final List<CoreFactory> coreFactories;

  public CoreModule(List<CoreFactory> coreFactories) {
    assert coreFactories.size() > 0;
    this.coreFactories = coreFactories;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public CoreMatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Core> cores = new ImmutableList.Builder<>();
    for (CoreFactory factory : this.coreFactories) {
      Core core = new Core(factory, match);
      match.getFeatureContext().add(core);
      match.needModule(GoalMatchModule.class).addGoal(core);
      cores.add(core);
    }

    return new CoreMatchModule(match, cores.build());
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<CoreModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(ObjectiveModesModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
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
            Teams.getTeam(new Node(XMLUtils.getRequiredAttribute(coreEl, "team")), context);
        Region region;
        RegionParser parser = context.getRegions();
        if (context.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
          region = parser.parseChildren(coreEl);
          parser.validate(region, BlockBoundedValidation.INSTANCE, new Node(coreEl));
        } else {
          region = parser.parseRequiredProperty(coreEl, "region", BlockBoundedValidation.INSTANCE);
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

        ImmutableSet<Mode> modeSet;
        Node modes = Node.fromAttr(coreEl, "modes");
        if (modes != null) {
          if (coreEl.getAttribute("mode-changes") != null) {
            throw new InvalidXMLException("Cannot combine modes and mode-changes", coreEl);
          }
          modeSet = parseModeSet(context, modes); // Specific set of modes
        } else if (XMLUtils.parseBoolean(coreEl.getAttribute("mode-changes"), false)) {
          modeSet = null; // All modes
        } else {
          modeSet = ImmutableSet.of(); // No modes
        }

        boolean showProgress = XMLUtils.parseBoolean(coreEl.getAttribute("show-progress"), false);
        ShowOptions options = ShowOptions.parse(context.getFilters(), coreEl);
        Boolean required = XMLUtils.parseBoolean(coreEl.getAttribute("required"), null);
        ProximityMetric proximityMetric =
            ProximityMetric.parse(
                coreEl, new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));

        CoreFactory factory =
            new CoreFactory(
                id,
                name,
                required,
                options,
                owner,
                proximityMetric,
                region,
                material,
                leakLevel,
                modeSet,
                showProgress);
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

    public ImmutableSet<Mode> parseModeSet(MapFactory factory, Node node)
        throws InvalidXMLException {
      ImmutableSet.Builder<Mode> modes = ImmutableSet.builder();
      for (String modeId : node.getValue().split("\\s")) {
        Mode mode = factory.getFeatures().get(modeId, Mode.class);
        if (mode == null) {
          throw new InvalidXMLException("No mode with ID '" + modeId + "'", node);
        }
        modes.add(mode);
      }
      return modes.build();
    }
  }
}
