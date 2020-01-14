package tc.oc.pgm.api.module;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A contextual collection of {@link Module}s indexed by their class.
 */
public interface ModuleContext<M extends Module> {

  /**
   * Get a map of {@link Module}s indexed by their class.
   *
   * @return A map of {@link Module}s.
   */
  Map<Class<? extends M>, M> getModules();

  /**
   * Get a specific {@link Module} from its class.
   *
   * @param key A specific {@link Module} class.
   * @return A {@link Module} or {@code null} if not found.
   */
  @Nullable
  default <N extends M> N getModule(Class<? extends N> key) {
    return (N) getModules().get(key);
  }

  /**
   * Require a specific {@link Module} from its class.
   *
   * @param key A specific {@link Module} class.
   * @return A {@link Module}.
   * @throws IllegalStateException If not found.
   */
  default <N extends M> N needModule(Class<? extends N> key) {
    final N module = getModule(key);
    if (module == null) {
      throw new IllegalStateException(key.getSimpleName() + " was required, but not found");
    }
    return module;
  }

  /**
   * Get whether this context contains a particular {@link Module}.
   *
   * @param key A specific {@link Module} class.
   * @return Whether the {@link Module} exists.
   */
  default boolean hasModule(Class<? extends M> key) {
    return getModule(key) != null;
  }
}
