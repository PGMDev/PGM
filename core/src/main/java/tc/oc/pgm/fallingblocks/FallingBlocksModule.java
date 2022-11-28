package tc.oc.pgm.fallingblocks;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class FallingBlocksModule implements MapModule<FallingBlocksMatchModule> {
  private final List<FallingBlocksRule> rules;

  public FallingBlocksModule(List<FallingBlocksRule> rules) {
    this.rules = rules;
  }

  @Override
  public FallingBlocksMatchModule createMatchModule(Match match) {
    return new FallingBlocksMatchModule(match, this.rules);
  }

  public static class Factory implements MapModuleFactory<FallingBlocksModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(FilterModule.class, RegionModule.class);
    }

    @Override
    public FallingBlocksModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<FallingBlocksRule> rules = new ArrayList<>();
      FilterParser filterParser = factory.getFilters();

      for (Element elRule :
          XMLUtils.flattenElements(doc.getRootElement(), "falling-blocks", "rule")) {
        Filter fall = filterParser.parseRequiredFilterProperty(elRule, "filter");
        Filter stick = filterParser.parseFilterProperty(elRule, "sticky", StaticFilter.DENY);
        int delay =
            XMLUtils.parseNumber(
                Node.fromChildOrAttr(elRule, "delay"),
                Integer.class,
                FallingBlocksRule.DEFAULT_DELAY);

        rules.add(new FallingBlocksRule(fall, stick, delay));
      }
      return rules.isEmpty() ? null : new FallingBlocksModule(rules);
    }
  }
}
