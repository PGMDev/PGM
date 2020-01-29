package tc.oc.pgm.filters;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
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
    if (match.getMap().getProto().isOlderThan(MapProtos.FILTER_FEATURES)) {
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
    public FilterModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean unified = factory.getProto().isNoOlderThan(MapProtos.FILTER_FEATURES);
      FilterParser parser = factory.getFilters();

      if (unified) {
        factory.getFeatures().addFeature(null, "always", StaticFilter.ALLOW);
        factory.getFeatures().addFeature(null, "never", StaticFilter.DENY);
        factory.getFeatures().addFeature(null, "everywhere", EverywhereRegion.INSTANCE);
        factory.getFeatures().addFeature(null, "nowhere", EmptyRegion.INSTANCE);
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
