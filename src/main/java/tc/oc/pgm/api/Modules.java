package tc.oc.pgm.api;

import java.util.Map;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;

public interface Modules {

  Map<Class<? extends MapModule>, MapModuleFactory<? extends MapModule>> getMapModuleFactories();

  Map<Class<? extends MatchModule>, MatchModuleFactory<? extends MatchModule>> getModuleFactories();
}
