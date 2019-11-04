package tc.oc.pgm.regions;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Regions",
    requires = {FilterModule.class, KitModule.class})
public class RegionModule extends MapModule {
  protected final RFAContext rfaContext;

  public RegionModule(RFAContext rfaContext) {
    this.rfaContext = rfaContext;
  }

  public RFAContext getRFAContext() {
    return rfaContext;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RegionMatchModule(match, this.rfaContext);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static RegionModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    RegionParser parser = context.getRegionParser();
    boolean unified = context.getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES);

    // If proto >= 1.4 then the filter module will parse all regions
    if (!unified) {
      for (Element regionRootElement : doc.getRootElement().getChildren("regions")) {
        parser.parseSubRegions(regionRootElement);
      }
    }

    // parse filter applications
    RFAContext rfaContext = new RFAContext();
    RegionFilterApplicationParser rfaParser =
        new RegionFilterApplicationParser(context, rfaContext);

    for (Element regionRootElement : doc.getRootElement().getChildren("regions")) {
      for (Element applyEl : regionRootElement.getChildren("apply")) {
        rfaParser.parse(applyEl);
      }
    }

    // Proto 1.4+ <apply> can appear in <regions> or <filters>
    if (unified) {
      for (Element regionRootElement : doc.getRootElement().getChildren("filters")) {
        for (Element applyEl : regionRootElement.getChildren("apply")) {
          rfaParser.parse(applyEl);
        }
      }
    }

    return new RegionModule(rfaContext);
  }

  @Override
  public void postParse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    for (RegionFilterApplication rfa : context.features().getAll(RegionFilterApplication.class)) {
      if (rfa.lendKit && !rfa.kit.isRemovable()) {
        throw new InvalidXMLException(
            "Specified lend-kit is not removable", context.features().getNode(rfa));
      }
    }
  }
}
