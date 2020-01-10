package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class MaxBuildHeightModule implements MapModule {
  private final int buildHeight;

  public MaxBuildHeightModule(int buildHeight) {
    this.buildHeight = buildHeight;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new MaxBuildHeightMatchModule(match, this.buildHeight);
  }

  public static class Factory implements MapModuleFactory<MaxBuildHeightModule> {
    @Override
    public MaxBuildHeightModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      Element maxBuildHeightEl = XMLUtils.getUniqueChild(doc.getRootElement(), "maxbuildheight");
      if (maxBuildHeightEl == null) {
        return null;
      } else {
        return new MaxBuildHeightModule(XMLUtils.parseNumber(maxBuildHeightEl, Integer.class));
      }
    }
  }
}
