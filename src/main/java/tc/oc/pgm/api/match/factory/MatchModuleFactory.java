package tc.oc.pgm.api.match.factory;

import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

/**
 * A factory for a {@link MatchModule}.
 *
 * <p>Unlike a {@link tc.oc.pgm.api.map.MapModule}, this does not require a {@link
 * org.jdom2.Document}.
 *
 * @param <T> The specific type of {@link MatchModule}.
 */
@FunctionalInterface
public interface MatchModuleFactory<T extends MatchModule> {

  /**
   * Creates a {@link MatchModule} for a particular {@link Match}.
   *
   * @param match The {@link Match}.
   * @return The {@link MatchModule}, or {@code null} to silently fail.
   * @throws ModuleLoadException If there was an error creating the {@link MatchModule}.
   */
  @Nullable
  T createMatchModule(Match match) throws ModuleLoadException;
}
