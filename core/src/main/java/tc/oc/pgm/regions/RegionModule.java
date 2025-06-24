package tc.oc.pgm.regions;

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
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.kits.KitModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class RegionModule implements MapModule<RegionMatchModule> {
  // Can't be final as this is initially the builder, later replaced by the built copy.
  private RFAContext rfaContext;
  private final Integer maxBuildHeight;

  public RegionModule(RFAContext rfaContext, Integer maxBuildHeight) {
    this.rfaContext = rfaContext;
    this.maxBuildHeight = maxBuildHeight;
  }

  public RFAContext.Builder getRFAContextBuilder() {
    if (rfaContext instanceof RFAContext.Builder) return (RFAContext.Builder) rfaContext;
    throw new UnsupportedOperationException("Cannot get RFA builder at this stage.");
  }

  @Override
  public RegionMatchModule createMatchModule(Match match) {
    return new RegionMatchModule(match, this.rfaContext, maxBuildHeight);
  }

  public static class Factory implements MapModuleFactory<RegionModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(FilterModule.class, KitModule.class);
    }

    @Override
    public RegionModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      RegionParser parser = factory.getRegions();
      boolean unified = factory.getProto().isNoOlderThan(MapProtos.FILTER_FEATURES);

      // If proto >= 1.4 then the filter module will parse all regions
      if (!unified) {
        for (Element regionRootElement : doc.getRootElement().getChildren("regions")) {
          parser.parseSubRegions(regionRootElement);
        }
      }

      // parse filter applications
      RFAContext.Builder rfaContext = new RFAContext.Builder();
      RegionFilterApplicationParser rfaParser =
          new RegionFilterApplicationParser(factory, rfaContext);

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

      // Support legacy <lanes> syntax
      for (Element laneEl : XMLUtils.flattenElements(doc.getRootElement(), "lanes", "lane")) {
        rfaParser.parseLane(laneEl);
      }

      // Support deprecated <playable> syntax
      if (factory.getProto().isOlderThan(MapProtos.MODULE_SUBELEMENT_VERSION)) {
        Element playableEl = XMLUtils.getUniqueChild(doc.getRootElement(), "playable");
        if (playableEl != null) rfaParser.parsePlayable(playableEl);
      }

      // Support <maxbuildheight> syntax
      Integer maxBuild =
          rfaParser.parseMaxBuildHeight(
              XMLUtils.getUniqueChild(doc.getRootElement(), "maxbuildheight"));

      return new RegionModule(rfaContext, maxBuild);
    }
  }

  @Override
  public void postParse(MapFactory factory, Logger logger, Document doc)
      throws InvalidXMLException {

    rfaContext = getRFAContextBuilder().build();

    for (RegionFilterApplication rfa :
        factory.getFeatures().getAll(RegionFilterApplication.class)) {
      if (rfa.lendKit && !rfa.kit.isRemovable()) {
        throw new InvalidXMLException(
            "Specified lend-kit is not removable", factory.getFeatures().getNode(rfa));
      }
    }
  }
}
