package tc.oc.pgm.enderchest;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class EnderChestModule implements MapModule {

  private final boolean enabled;
  private final int rows;
  private final List<Dropoff> dropoffs;

  public EnderChestModule(boolean enabled, int rows, List<Dropoff> dropoffs) {
    this.enabled = enabled;
    this.rows = rows;
    this.dropoffs = dropoffs;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new EnderChestMatchModule(match, enabled, rows, dropoffs);
  }

  public static class Factory implements MapModuleFactory<EnderChestModule> {
    @Override
    public EnderChestModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filters = factory.getFilters();
      RegionParser regions = factory.getRegions();

      boolean enabled = false;
      int rows = 3;
      List<Dropoff> dropoffs = Lists.newArrayList();

      for (Element enderRootEl : doc.getRootElement().getChildren("enderchest")) {

        dropoffs = Lists.newArrayList();
        for (Element dropoffEl : XMLUtils.getChildren(enderRootEl, "dropoff")) {
          Region region = regions.parseRegionProperty(dropoffEl, "region");
          Filter filter = filters.parseFilterProperty(dropoffEl, "filter");

          if (region == null || filter == null)
            throw new InvalidXMLException("Dropoffs require both a region and filter", dropoffEl);

          dropoffs.add(new Dropoff(region, filter));
        }

        Attribute rowAttr = XMLUtils.getAttribute(enderRootEl, "rows");
        if (rowAttr != null) rows = XMLUtils.parseNumber(rowAttr, Integer.class);

        if (rows < 1 || rows > 6)
          throw new InvalidXMLException("Row amount must be between 1 and 6", enderRootEl);

        enabled = true;
      }

      return new EnderChestModule(enabled, rows, dropoffs);
    }
  }
}
