package tc.oc.pgm.platform.modern.action.actions;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGHEST;
import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.action.actions.ActionParserProvider;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.21.11", priority = HIGHEST)
public class ModernActionParserProvider implements ActionParserProvider {
  @Override
  public ActionParser create(MapFactory factory) {
    return new ModernActionParser(factory);
  }
}
