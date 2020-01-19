package tc.oc.pgm.api.match;

import tc.oc.pgm.api.module.Module;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

/** An immutable code module that loads and unloads during a {@link Match}. */
public interface MatchModule extends Module {

  /** Callback when {@link Match#load()} is executed. */
  default void load() throws ModuleLoadException {}

  /** Callback when {@link Match#start()} is executed. */
  default void enable() {}

  /** Callback when {@link Match#finish()} is executed. */
  default void disable() {}

  /** Callback when {@link Match#unload()} is executed. */
  default void unload() {}
}
