package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.regions.EmptyRegion;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.xml.InvalidXMLException;

public class FilterModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    if (match.getMapContext().getInfo().getProto().isOlderThan(ProtoVersions.FILTER_FEATURES)) {
      return null;
    } else {
      return new FilterMatchModule(match);
    }
  }

  public static class Factory implements MapModuleFactory<FilterModule> {
    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(TeamModule.class, ClassModule.class);
    }

    @Override
    public FilterModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean unified = context.getInfo().getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES);
      FilterParser parser = context.legacy().getFilters();

      if (unified) {
        context.legacy().getFeatures().addFeature(null, "always", StaticFilter.ALLOW);
        context.legacy().getFeatures().addFeature(null, "never", StaticFilter.DENY);
        context.legacy().getFeatures().addFeature(null, "everywhere", EverywhereRegion.INSTANCE);
        context.legacy().getFeatures().addFeature(null, "nowhere", EmptyRegion.INSTANCE);
      }

      for (Element filtersEl : doc.getRootElement().getChildren("filters")) {
        parser.parseFilterChildren(filtersEl);
      }

      if (unified) {
        for (Element filtersEl : doc.getRootElement().getChildren("regions")) {
          parser.parseFilterChildren(filtersEl);
        }
      }

      return new FilterModule();
    }
  }
}
