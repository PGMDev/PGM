package tc.oc.pgm.modules;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.ProtoVersions;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class MobsModule implements MapModule {
  private final Filter mobsFilter;

  public MobsModule(Filter mobsFilter) {
    this.mobsFilter = mobsFilter;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new MobsMatchModule(match, this.mobsFilter);
  }

  public static class Factory implements MapModuleFactory<MobsModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public MobsModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      FilterParser filterParser = factory.getFilters();
      Element mobsEl = doc.getRootElement().getChild("mobs");
      Filter mobsFilter = StaticFilter.DENY;
      if (mobsEl != null) {
        if (factory.getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES)) {
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
