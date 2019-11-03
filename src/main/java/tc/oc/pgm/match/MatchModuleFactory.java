package tc.oc.pgm.match;

import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.module.ModuleLoadException;

/**
 * Creates a {@link MatchModule} to be loaded into a {@link Match}, or returns null to indicate that
 * the module is not needed.
 */
public interface MatchModuleFactory<T extends MatchModule> {
  @Nullable
  T createMatchModule(Match match) throws ModuleLoadException;
}
