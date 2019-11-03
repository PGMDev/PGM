package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "FriendlyFireRefund")
public class FriendlyFireRefundModule extends MapModule {
  @Override
  public MatchModule createMatchModule(Match match) {
    return new FriendlyFireRefundMatchModule(match);
  }

  public static FriendlyFireRefundModule parse(
      MapModuleContext context, Logger logger, Document doc) throws InvalidXMLException {
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
