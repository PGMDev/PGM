package tc.oc.pgm.picker;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.classes.ClassModule;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "picker",
    follows = {TeamModule.class, FreeForAllModule.class, ClassModule.class})
public class PickerModule extends MapModule<PickerMatchModule> {

  @Override
  public PickerMatchModule createMatchModule(Match match) {
    return new PickerMatchModule(match);
  }

  public static PickerModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    if (context.hasModule(FreeForAllModule.class)
        || context.hasModule(TeamModule.class)
        || context.hasModule(ClassModule.class)) {
      return new PickerModule();
    } else {
      return null;
    }
  }
}
