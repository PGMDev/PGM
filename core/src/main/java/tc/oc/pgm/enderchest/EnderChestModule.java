package tc.oc.pgm.enderchest;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.RandomPointsValidation;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class EnderChestModule implements MapModule<EnderChestMatchModule> {

  private final boolean enabled;
  private final List<Dropoff> dropoffs;
  private final DropoffFallback fallback;

  public EnderChestModule(boolean enabled, List<Dropoff> dropoffs, DropoffFallback fallback) {
    this.enabled = enabled;
    this.dropoffs = dropoffs;
    this.fallback = fallback;
  }

  @Override
  public EnderChestMatchModule createMatchModule(Match match) {
    return new EnderChestMatchModule(match, enabled, dropoffs, fallback);
  }

  public static class Factory implements MapModuleFactory<EnderChestModule> {
    @Override
    public EnderChestModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filters = factory.getFilters();
      RegionParser regions = factory.getRegions();

      boolean enabled = false;
      DropoffFallback fallback = DropoffFallback.AUTO;
      List<Dropoff> dropoffs = Lists.newArrayList();

      for (Element enderchestEl : doc.getRootElement().getChildren("enderchest")) {
        fallback =
            XMLUtils.parseEnum(
                Node.fromAttr(enderchestEl, "fallback"),
                DropoffFallback.class,
                "fallback",
                DropoffFallback.AUTO);
        enabled = true;
      }

      for (Element dropoffEl :
          XMLUtils.flattenElements(doc.getRootElement(), "enderchest", "dropoff")) {
        Region region =
            regions.parseRequiredProperty(dropoffEl, "region", RandomPointsValidation.INSTANCE);
        Filter filter = filters.parseRequiredProperty(dropoffEl, "filter");
        dropoffs.add(new Dropoff(region, filter));
      }

      return new EnderChestModule(enabled, dropoffs, fallback);
    }
  }
}
