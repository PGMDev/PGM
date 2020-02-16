package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

/**
 * Assorted features used by internal maps i.e. maps that have no outer surface. We assume that such
 * maps have a bedrock outfill spanning the full world height.
 */
public class InternalModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new InternalMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<InternalModule> {
    @Override
    public InternalModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      if (XMLUtils.parseBoolean(
          Node.fromLastChildOrAttr(doc.getRootElement(), "internal"), false)) {
        return new InternalModule();
      }
      return null;
    }
  }
}
