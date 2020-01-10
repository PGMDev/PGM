package tc.oc.pgm.hunger;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.xml.InvalidXMLException;

public class HungerModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new HungerMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<HungerModule> {
    @Override
    public HungerModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean on = true;

      for (Element hungerRootElement : doc.getRootElement().getChildren("hunger")) {
        Element hungerDepletionElement = hungerRootElement.getChild("depletion");
        if (hungerDepletionElement != null) {
          if (hungerDepletionElement.getValue().equalsIgnoreCase("off")) {
            on = false;
          }
        }
      }

      if (!on) {
        return new HungerModule();
      } else {
        return null;
      }
    }
  }
}
