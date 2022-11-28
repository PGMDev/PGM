package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class MobsModule implements MapModule<MobsMatchModule> {

  private final Filter mobsFilter;

  public MobsModule(Filter mobsFilter) {
    this.mobsFilter = mobsFilter;
  }

  @Override
  public MobsMatchModule createMatchModule(Match match) {
    return new MobsMatchModule(match, this.mobsFilter);
  }

  public static class Factory implements MapModuleFactory<MobsModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public MobsModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filterParser = factory.getFilters();
      Element mobsEl = doc.getRootElement().getChild("mobs");
      Filter mobsFilter = null;
      if (mobsEl != null) {
        if (factory.getProto().isNoOlderThan(MapProtos.FILTER_FEATURES)) {
          mobsFilter = filterParser.parseRequiredFilterProperty(mobsEl, "filter");
        } else {
          Element filterEl = XMLUtils.getUniqueChild(mobsEl, "filter");
          if (filterEl != null) {
            mobsFilter = filterParser.parse(filterEl);
          }
        }
      }
      return new MobsModule(mobsFilter);
    }
  }
}
