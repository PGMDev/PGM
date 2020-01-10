package tc.oc.pgm.api.match;

import tc.oc.pgm.api.module.Module;

public interface MatchModule extends Module {

  default void enable() {}

  default void disable() {}
}
