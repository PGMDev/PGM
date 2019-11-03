package tc.oc.pgm.filters;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.pgm.regions.EmptyRegion;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Filters",
    follows = {TeamModule.class, ClassModule.class})
public class FilterModule extends MapModule {

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    if (match.getMapContext().getProto().isOlderThan(ProtoVersions.FILTER_FEATURES)) {
      return null;
    } else {
      return new FilterMatchModule(match);
    }
  }

  public static FilterModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    boolean unified = context.getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES);
    FilterParser parser = context.getFilterParser();

    if (unified) {
      context.features().addFeature(null, "always", StaticFilter.ALLOW);
      context.features().addFeature(null, "never", StaticFilter.DENY);
      context.features().addFeature(null, "everywhere", EverywhereRegion.INSTANCE);
      context.features().addFeature(null, "nowhere", EmptyRegion.INSTANCE);
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
