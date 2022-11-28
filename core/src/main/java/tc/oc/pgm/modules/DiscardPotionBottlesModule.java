package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class DiscardPotionBottlesModule implements MapModule<DiscardPotionBottlesMatchModule> {

  @Override
  public DiscardPotionBottlesMatchModule createMatchModule(Match match) {
    return new DiscardPotionBottlesMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<DiscardPotionBottlesModule> {
    @Override
    public DiscardPotionBottlesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element el = doc.getRootElement().getChild("keep-potion-bottles");
      if (el == null) {
        return new DiscardPotionBottlesModule();
      }

      return null;
    }
  }
}
