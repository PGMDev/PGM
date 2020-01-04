package tc.oc.pgm.modules;

import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

/**
 * Assorted features used by internal maps i.e. maps that have no outer surface. We assume that such
 * maps have a bedrock outfill spanning the full world height.
 */
@ModuleDescription(name = "Internal Topology Module")
public class InternalModule extends MapModule {

  private static final MapTag INTERNAL_TAG = MapTag.forName("internal");

  @Override
  @SuppressWarnings("unchecked")
  public void loadTags(Set tags) {
    tags.add(INTERNAL_TAG);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new InternalMatchModule(match);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static InternalModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    if (XMLUtils.parseBoolean(Node.fromLastChildOrAttr(doc.getRootElement(), "internal"), false)) {
      return new InternalModule();
    }
    return null;
  }
}
