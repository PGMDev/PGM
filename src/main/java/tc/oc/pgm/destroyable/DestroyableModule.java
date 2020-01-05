package tc.oc.pgm.destroyable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.GoalModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.BlockBoundedValidation;
import tc.oc.pgm.regions.Region;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(
    name = "Destroyable",
    depends = {TeamModule.class, RegionModule.class, BlockDropsModule.class, GoalModule.class})
public class DestroyableModule extends MapModule<DestroyableMatchModule> {
  private static final MapTag MONUMENT_TAG = MapTag.forName("monumnet");

  protected final List<DestroyableFactory> destroyableFactories;

  public DestroyableModule(List<DestroyableFactory> destroyableFactories) {
    this.destroyableFactories = destroyableFactories;
  }

  @Override
  public void loadTags(Set<MapTag> tags) {
    tags.add(MONUMENT_TAG);
  }

  @Override
  public DestroyableMatchModule createMatchModule(Match match) {
    ImmutableList.Builder<Destroyable> destroyables = new ImmutableList.Builder<>();
    for (DestroyableFactory factory : this.destroyableFactories) {
      Destroyable destroyable = new Destroyable(factory, match);
      match.needMatchModule(GoalMatchModule.class).addGoal(destroyable);
      match.getFeatureContext().add(destroyable);
      destroyables.add(destroyable);
    }
    return new DestroyableMatchModule(match, destroyables.build());
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static DestroyableModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<DestroyableFactory> destroyables = Lists.newArrayList();
    TeamModule teamModule = context.getModule(TeamModule.class);
    RegionParser regionParser = context.getRegionParser();

    for (Element destroyableEl :
        XMLUtils.flattenElements(
            doc.getRootElement(),
            ImmutableSet.of("destroyables", "giraffes"),
            ImmutableSet.of("destroyable", "giraffe"))) {
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
      if (context.getProto().isOlderThan(ProtoVersions.MODULE_SUBELEMENT_VERSION)) {
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

      context.features().addFeature(destroyableEl, factory);
      destroyables.add(factory);
    }

    if (destroyables.size() > 0) {
      return new DestroyableModule(destroyables);
    } else {
      return null;
    }
  }
}
