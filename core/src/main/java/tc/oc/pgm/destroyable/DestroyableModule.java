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
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.blockdrops.BlockDropsModule;
import tc.oc.pgm.destroyable.DestroyableFactory.SparksType;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityMetric;
import tc.oc.pgm.goals.ShowOptions;
import tc.oc.pgm.modes.Mode;
import tc.oc.pgm.modes.ObjectiveModesModule;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class DestroyableModule implements MapModule<DestroyableMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("monument", Gamemode.DESTROY_THE_MONUMENT, false));
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
      var parser = context.getParser();

      for (Element el : XMLUtils.flattenElements(
          doc.getRootElement(), Set.of("destroyables"), Set.of("destroyable"))) {
        var owner = parser.node(n -> Teams.getTeam(n, context), el, "owner").required();
        String name = parser.string(el, "name").required();

        double completion = parser.node(this::parsePercent, el, "completion").optional(1.0);

        var region = parser.region(el, "region").legacy(context).blockBounded().required();

        String id = parser.string(el, "id").orNull();
        MaterialMatcher materials = MaterialMatcher.builder()
            .blocksOnly()
            .multiPattern()
            .parse(Node.fromRequiredAttr(el, "materials", "material"))
            .build();

        var modeSet = parser.node(n -> parseModeSet(context, n), el, "modes").orNull();
        var modeChanges = parser.parseBool(el, "mode-changes").orNull();
        if (modeChanges != null) {
          if (modeSet != null)
            throw new InvalidXMLException("Cannot combine modes and mode-changes", el);
          modeSet = modeChanges ? null : ImmutableSet.of();
        }

        boolean showProgress = parser.parseBool(el, "show-progress").orFalse();
        SparksType sparks =
            parser.parseEnum(SparksType.class, el, "sparks").optional(SparksType.NONE);
        boolean repairable = parser.parseBool(el, "repairable").orTrue();
        ShowOptions options = ShowOptions.parse(context.getFilters(), el);
        Boolean required = parser.parseBool(el, "required").orNull();
        ProximityMetric proximityMetric = ProximityMetric.parse(
            el, new ProximityMetric(ProximityMetric.Type.CLOSEST_PLAYER, false));

        DestroyableFactory factory = new DestroyableFactory(
            id,
            name,
            required,
            options,
            owner,
            proximityMetric,
            region,
            materials,
            completion,
            modeSet,
            showProgress,
            sparks,
            repairable);

        context.getFeatures().addFeature(el, factory);
        destroyables.add(factory);
      }

      if (!destroyables.isEmpty()) {
        return new DestroyableModule(destroyables);
      } else {
        return null;
      }
    }

    public double parsePercent(Node n) throws InvalidXMLException {
      return XMLUtils.parseNumber(n, n.getValue().replace("%", "").trim(), Double.class) / 100.0;
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
