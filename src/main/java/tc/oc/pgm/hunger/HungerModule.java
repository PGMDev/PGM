package tc.oc.pgm.hunger;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;

@ModuleDescription(name = "Hunger")
public class HungerModule extends MapModule {
  @Override
  public MatchModule createMatchModule(Match match) {
    return new HungerMatchModule(match);
  }

  public static HungerModule parse(MapModuleContext context, Logger logger, Document doc) {
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
