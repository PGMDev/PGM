package tc.oc.pgm.api.module;

import tc.oc.pgm.api.module.exception.ModuleLoadException;

public interface Module {

  default void load() throws ModuleLoadException {}

  default void unload() {}
}
