package tc.oc.pgm.module;

import java.util.logging.Logger;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.match.MatchModule;

/**
 * If a {@link MapModule}s or {@link MatchModule}s implements this interface, the {@link #register}
 * method will be called when the module is registered with a {@link ModuleRegistry}.
 */
public interface Registerable {
  void register(ModuleRegistry context, Logger logger) throws Throwable;
}
