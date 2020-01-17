package tc.oc.pgm.api.match.factory;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

import javax.annotation.Nullable;

/**
 * A factory for creating {@link MatchModule}s.
 */
@FunctionalInterface
public interface MatchModuleFactory<T extends MatchModule> {

  /**
   * Creates a {@link MatchModule} for a particular {@link Match}.
   *
   * @param match The {@link Match}.
   * @return The {@link MatchModule}, or {@code null} to silently skip.
   * @throws ModuleLoadException If there was an error creating the {@link MatchModule}.
   */
  @Nullable
  T createMatchModule(Match match) throws ModuleLoadException;
}
