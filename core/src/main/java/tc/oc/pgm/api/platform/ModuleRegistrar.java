package tc.oc.pgm.api.platform;

import tc.oc.pgm.api.Modules;
import tc.oc.pgm.util.platform.Platform;

/** An interface for a module registrar */
public interface ModuleRegistrar {
  ModuleRegistrar MODULE_REGISTRAR = Platform.get(ModuleRegistrar.class);

  void registerModules(Modules modules);
}
