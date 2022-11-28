package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class FriendlyFireRefundModule implements MapModule<FriendlyFireRefundMatchModule> {
  @Override
  public FriendlyFireRefundMatchModule createMatchModule(Match match) {
    return new FriendlyFireRefundMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<FriendlyFireRefundModule> {
    @Override
    public FriendlyFireRefundModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean on =
          XMLUtils.parseBoolean(
              Node.fromLastChildOrAttr(
                  doc.getRootElement(), "friendly-fire-refund", "friendlyfirerefund"),
              true);

      if (on) {
        return new FriendlyFireRefundModule();
      } else {
        return null;
      }
    }
  }
}
