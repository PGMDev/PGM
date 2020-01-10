package tc.oc.pgm.api.module;

import java.util.Map;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.factory.MatchModuleFactory;

public interface ModuleRegistry {

  Map<Class<? extends MapModule>, MapModuleFactory> getMapModuleFactories();

  Map<Class<? extends MatchModule>, MatchModuleFactory> getMatchModuleFactories();
}
