package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
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
import tc.oc.pgm.blockdrops.BlockDropsModule;
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
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class DestroyableModule implements MapModule<DestroyableMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("dtm", "monument", "Destroy the Monument", true, false));
  protected final List<DestroyableFactory> destroyableFactories;

  public DestroyableModule(List<DestroyableFactory> destroyableFactories) {
    this.destroyableFactories = destroyableFactories;
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public DestroyableMatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Destroyable> destroyables = new ImmutableList.Builder<>();
    for (DestroyableFactory factory : this.destroyableFactories) {
      Destroyable destroyable = new Destroyable(factory, match);
      match.needModule(GoalMatchModule.class).addGoal(destroyable);
      match.getFeatureContext().add(destroyable);
      destroyables.add(destroyable);
    }
    return new DestroyableMatchModule(match, destroyables.build());
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<DestroyableModule> {

    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(BlockDropsModule.class, ObjectiveModesModule.class);
    }

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(TeamModule.class, RegionModule.class);
    }

    @Override
    public DestroyableModule parse(MapFactory context, Logger logger, Document doc)
        throws InvalidXMLException {
      List<DestroyableFactory> destroyables = Lists.newArrayList();
      RegionParser regionParser = context.getRegions();

      for (Element destroyableEl :
          XMLUtils.flattenElements(
              doc.getRootElement(),
              ImmutableSet.of("destroyables"),
              ImmutableSet.of("destroyable"))) {
        TeamFactory owner =
            Teams.getTeam(new Node(XMLUtils.getRequiredAttribute(destroyableEl, "owner")), context);
        String name = XMLUtils.getRequiredAttribute(destroyableEl, "name").getValue();

        double destructionRequired = 1.0;
        String destructionRequiredText = destroyableEl.getAttributeValue("completion");
        if (destructionRequiredText != null) {
          destructionRequired =
              Double.parseDouble(destructionRequiredText.replace("%", "").trim()) / 100.0d;
        }

        Region region;
        if (context.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
          region = regionParser.parseChildren(destroyableEl);
          regionParser.validate(region, BlockBoundedValidation.INSTANCE, new Node(destroyableEl));
        } else {
          region =
              regionParser.parseRequiredProperty(
                  destroyableEl, "region", BlockBoundedValidation.INSTANCE);
        }

        String id = destroyableEl.getAttributeValue("id");
        Set<SingleMaterialMatcher> materials =
            XMLUtils.parseMaterialPatternSet(
                Node.fromRequiredAttr(destroyableEl, "materials", "material"));

        ImmutableSet<Mode> modeSet;
        Node modes = Node.fromAttr(destroyableEl, "modes");
        if (modes != null) {
          if (destroyableEl.getAttribute("mode-changes") != null) {
            throw new InvalidXMLException("Cannot combine modes and mode-changes", destroyableEl);
          }
          modeSet = parseModeSet(context, modes); // Specific set of modes
        } else if (XMLUtils.parseBoolean(destroyableEl.getAttribute("mode-changes"), false)) {
          modeSet = null; // All modes
        } else {
          modeSet = ImmutableSet.of(); // No modes
        }

        boolean showProgress =
            XMLUtils.parseBoolean(destroyableEl.getAttribute("show-progress"), false);
        boolean sparks = XMLUtils.parseBoolean(destroyableEl.getAttribute("sparks"), false);
        boolean repairable = XMLUtils.parseBoolean(destroyableEl.getAttribute("repairable"), true);
        ShowOptions options = ShowOptions.parse(context.getFilters(), destroyableEl);
        Boolean required = XMLUtils.parseBoolean(destroyableEl.getAttribute("required"), null);
        ProximityMetric proximityMetric =
            ProximityMetric.parse(
                destroyableEl, new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));

        DestroyableFactory factory =
            new DestroyableFactory(
                id,
                name,
                required,
                options,
                owner,
                proximityMetric,
                region,
                materials,
                destructionRequired,
                modeSet,
                showProgress,
                sparks,
                repairable);

        context.getFeatures().addFeature(destroyableEl, factory);
        destroyables.add(factory);
      }

      if (!destroyables.isEmpty()) {
        return new DestroyableModule(destroyables);
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
