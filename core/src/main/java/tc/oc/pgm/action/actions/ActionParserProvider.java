package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.map.factory.MapFactory;

public interface ActionParserProvider {
  ActionParser create(MapFactory factory);
}
