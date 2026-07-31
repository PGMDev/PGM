package tc.oc.pgm.hunger;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class HungerModule implements MapModule<HungerMatchModule> {
  private final boolean depletion;
  private final boolean replenishment;

  public HungerModule(boolean depletion, boolean replenishment) {
    this.depletion = depletion;
    this.replenishment = replenishment;
  }

  @Override
  public HungerMatchModule createMatchModule(Match match) {
    return new HungerMatchModule(match, depletion, replenishment);
  }

  public static class Factory implements MapModuleFactory<HungerModule> {
    @Override
    public HungerModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();

      boolean depletion = true;
      Boolean replenishment = null; // Optional, will default to depletion

      for (Element hungerEl : doc.getRootElement().getChildren("hunger")) {
        depletion = parser.parseBool(hungerEl, "depletion").optional(depletion);
        replenishment = parser.parseBool(hungerEl, "replenishment").optional(replenishment);
      }

      // Legacy behavior, replenishment defaults to depletion
      if (replenishment == null) replenishment = depletion;

      return depletion && replenishment ? null : new HungerModule(depletion, replenishment);
    }
  }
}
