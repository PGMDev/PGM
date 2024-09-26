package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.block.VoidFilter;
import tc.oc.pgm.filters.matcher.match.MatchPhaseFilter;
import tc.oc.pgm.filters.matcher.player.CanFlyFilter;
import tc.oc.pgm.filters.matcher.player.FlyingFilter;
import tc.oc.pgm.filters.matcher.player.GroundedFilter;
import tc.oc.pgm.filters.matcher.player.ParticipatingFilter;
import tc.oc.pgm.filters.matcher.player.PlayerMovementFilter;
import tc.oc.pgm.filters.matcher.player.PlayerStateFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.EmptyRegion;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.collection.ContextStore;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.variables.VariablesModule;

public class FilterModule implements MapModule<FilterMatchModule> {

  private final ContextStore<? super Filter> filterContext;

  /**
   * Create the FilterModule.
   *
   * @param filterContext the context where all {@link Filters} for the relevant match can be found.
   *     Important to find {@link ReactorFactory}s
   */
  private FilterModule(ContextStore<? super Filter> filterContext) {
    this.filterContext = filterContext;
  }

  @Override
  public FilterMatchModule createMatchModule(Match match) {
    return new FilterMatchModule(match, this.filterContext);
  }

  public static class Factory implements MapModuleFactory<FilterModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(VariablesModule.class, TeamModule.class, ClassModule.class);
    }

    @Override
    public FilterModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean unified = factory.getProto().isNoOlderThan(MapProtos.FILTER_FEATURES);
      boolean featureIds = factory.getProto().isNoOlderThan(MapProtos.FEATURE_SINGLETON_IDS);
      FilterParser parser = factory.getFilters();

      var features = factory.getFeatures();
      if (unified) {
        features.addFeature(null, "always", StaticFilter.ALLOW);
        features.addFeature(null, "never", StaticFilter.DENY);
        features.addFeature(null, "everywhere", EverywhereRegion.INSTANCE);
        features.addFeature(null, "nowhere", EmptyRegion.INSTANCE);
      }

      if (featureIds) {
        // Participating
        features.addFeature(null, "observing", ParticipatingFilter.OBSERVING);
        features.addFeature(null, "participating", ParticipatingFilter.PARTICIPATING);

        // Player state
        features.addFeature(null, "alive", PlayerStateFilter.ALIVE);
        features.addFeature(null, "dead", PlayerStateFilter.DEAD);

        // Match state
        features.addFeature(null, "match-idle", MatchPhaseFilter.IDLE);
        features.addFeature(null, "match-starting", MatchPhaseFilter.STARTING);
        features.addFeature(null, "match-running", MatchPhaseFilter.RUNNING);
        features.addFeature(null, "match-finished", MatchPhaseFilter.FINISHED);
        features.addFeature(null, "match-started", MatchPhaseFilter.STARTED);

        // Void
        features.addFeature(null, "void", VoidFilter.INSTANCE);

        // Player poses
        features.addFeature(null, "crouching", PlayerMovementFilter.CROUCHING);
        features.addFeature(null, "walking", PlayerMovementFilter.WALKING);
        features.addFeature(null, "sprinting", PlayerMovementFilter.SPRINTING);
        features.addFeature(null, "grounded", GroundedFilter.INSTANCE);
        features.addFeature(null, "flying", FlyingFilter.INSTANCE);
        features.addFeature(null, "can-fly", CanFlyFilter.INSTANCE);
      }

      for (Element filtersEl : doc.getRootElement().getChildren("filters")) {
        parser.parseFilterChildren(filtersEl);
      }

      if (unified) {
        for (Element filtersEl : doc.getRootElement().getChildren("regions")) {
          parser.parseFilterChildren(filtersEl);
        }
      }

      return new FilterModule(factory.getFilters().getUsedContext());
    }
  }
}
