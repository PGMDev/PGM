package tc.oc.pgm.platform.sportpaper.actions;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.action.actions.ActionParserProvider;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGHEST)
public class SportPaperActionParser implements ActionParserProvider {
  @Override
  public ActionParser create(MapFactory factory) {
    return new ActionParser(factory);
  }
}
