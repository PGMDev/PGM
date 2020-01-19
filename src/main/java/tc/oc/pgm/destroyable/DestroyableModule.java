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
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.map.MapInfoExtra;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.blockdrops.BlockDropsMatchModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class DestroyableModule implements MapModule, MapInfoExtra {
  protected final List<DestroyableFactory> destroyableFactories;

  public DestroyableModule(List<DestroyableFactory> destroyableFactories) {
    this.destroyableFactories = destroyableFactories;
  }

  @Override
  public Collection<Class> getWeakDependencies() {
    return ImmutableList.of(BlockDropsMatchModule.class);
  }

  @Override
  public Collection<Class<? extends MatchModule>> getSoftDependencies() {
    return ImmutableList.of(GoalMatchModule.class);
  }

  @Override
  public String getGenre() {
    return "Destroy the Monument";
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Destroyable> destroyables = new ImmutableList.Builder<>();
    for (DestroyableFactory factory : this.destroyableFactories) {
      Destroyable destroyable = new Destroyable(factory, match);
      match.needModule(GoalMatchModule.class).addGoal(destroyable);
      match.getFeatureContext().add(destroyable);
      destroyables.add(destroyable);
    }
    return new DestroyableMatchModule(match, destroyables.build());
  }

  public static class Factory implements MapModuleFactory<DestroyableModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(TeamModule.class, RegionModule.class, BlockDropsModule.class);
    }

    @Override
    public DestroyableModule parse(MapFactory context, Logger logger, Document doc)
        throws InvalidXMLException {
      List<DestroyableFactory> destroyables = Lists.newArrayList();
      TeamModule teamModule = context.getModule(TeamModule.class);
      RegionParser regionParser = context.getRegions();

      for (Element destroyableEl :
          XMLUtils.flattenElements(
              doc.getRootElement(),
              ImmutableSet.of("destroyables"),
              ImmutableSet.of("destroyable"))) {
        TeamFactory owner =
            teamModule.parseTeam(XMLUtils.getRequiredAttribute(destroyableEl, "owner"), context);
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
              regionParser.parseRequiredRegionProperty(
                  destroyableEl, BlockBoundedValidation.INSTANCE, "region");
        }

        String id = destroyableEl.getAttributeValue("id");
        Set<SingleMaterialMatcher> materials =
            XMLUtils.parseMaterialPatternSet(
                Node.fromRequiredAttr(destroyableEl, "materials", "material"));
        boolean modeChanges =
            XMLUtils.parseBoolean(destroyableEl.getAttribute("mode-changes"), false);
        boolean showProgress =
            XMLUtils.parseBoolean(destroyableEl.getAttribute("show-progress"), false);
        boolean sparks = XMLUtils.parseBoolean(destroyableEl.getAttribute("sparks"), false);
        boolean repairable = XMLUtils.parseBoolean(destroyableEl.getAttribute("repairable"), true);
        boolean visible = XMLUtils.parseBoolean(destroyableEl.getAttribute("show"), true);
        Boolean required = XMLUtils.parseBoolean(destroyableEl.getAttribute("required"), null);
        ProximityMetric proximityMetric =
            ProximityMetric.parse(
                destroyableEl, new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));

        DestroyableFactory factory =
            new DestroyableFactory(
                id,
                name,
                required,
                visible,
                owner,
                proximityMetric,
                region,
                materials,
                destructionRequired,
                modeChanges,
                showProgress,
                sparks,
                repairable);

        context.getFeatures().addFeature(destroyableEl, factory);
        destroyables.add(factory);
      }

      if (destroyables.size() > 0) {
        return new DestroyableModule(destroyables);
      } else {
        return null;
      }
    }
  }
}
