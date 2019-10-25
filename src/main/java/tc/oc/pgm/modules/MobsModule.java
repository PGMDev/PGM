package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.map.ProtoVersions;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "Mobs", depends = FilterModule.class)
public class MobsModule extends MapModule {
  private final Filter mobsFilter;

  public MobsModule(Filter mobsFilter) {
    this.mobsFilter = mobsFilter;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new MobsMatchModule(match, this.mobsFilter);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static MobsModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    FilterParser filterParser = context.getFilterParser();
    Element mobsEl = doc.getRootElement().getChild("mobs");
    Filter mobsFilter = StaticFilter.DENY;
    if (mobsEl != null) {
      if (context.getProto().isNoOlderThan(ProtoVersions.FILTER_FEATURES)) {
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
