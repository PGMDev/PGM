package tc.oc.pgm.api.module;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

/** A contextual collection of {@link Module}s indexed by their class. */
public interface ModuleContext<M extends Module> {

  /**
   * Get a sorted collection of {@link Module}s.
   *
   * @return A collection of {@link Module}s.
   */
  Collection<M> getModules();

  /**
   * Get a specific {@link Module} from its class.
   *
   * @param key A specific {@link Module} class.
   * @return A {@link Module} or {@code null} if not found.
   */
  @Nullable
  <N extends M> N getModule(Class<? extends N> key);

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
      throw new ModuleLoadException(key, "was required but not found");
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
