package tc.oc.pgm.fallingblocks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(
    name = "Falling Blocks",
    follows = {FilterModule.class, RegionModule.class})
public class FallingBlocksModule extends MapModule {
  private final List<FallingBlocksRule> rules;

  public FallingBlocksModule(List<FallingBlocksRule> rules) {
    this.rules = rules;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new FallingBlocksMatchModule(match, this.rules);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static FallingBlocksModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<FallingBlocksRule> rules = new ArrayList<>();
    FilterParser filterParser = context.getFilterParser();

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
