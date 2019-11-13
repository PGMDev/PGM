package tc.oc.pgm.ghostsquadron;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.classes.ClassMatchModule;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Ghost Squadron",
    depends = {ClassModule.class},
    follows = {BlitzModule.class})
public class GhostSquadronModule extends MapModule<GhostSquadronMatchModule> {

  @Override
  public Component getGame(MapModuleContext context) {
    return new PersonalizedTranslatable("match.scoreboard.gs.title");
  }

  @Override
  public GhostSquadronMatchModule createMatchModule(Match match) {
    return new GhostSquadronMatchModule(match, match.getMatchModule(ClassMatchModule.class));
  }

  public static GhostSquadronModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Element ghostSquadronEl = doc.getRootElement().getChild("ghostsquadron");
    if (ghostSquadronEl == null) {
      return null;
    } else {
      return new GhostSquadronModule();
    }
  }
}
