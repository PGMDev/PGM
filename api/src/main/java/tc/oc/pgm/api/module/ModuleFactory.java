package tc.oc.pgm.api.module;

import java.util.Collection;
import javax.annotation.Nullable;

/** A generic factory for creating {@link Module}s and their dependencies. */
public interface ModuleFactory<M extends Module> {

  /**
   * Get a collection of {@link Module} classes that must be loaded first.
   *
   * <p>If any fail to load, the entire dependency chain should fail.
   *
   * @return The factory's hard dependencies or {@code null} for none.
   */
  @Nullable
  default Collection<Class<? extends M>> getHardDependencies() {
    return null;
  }

  /**
   * Get a collection of {@link Module} classes that must be loaded first.
   *
   * <p>If any fail to load, the factory should silently fail and skip.
   *
   * @return The factory's soft dependencies or {@code null} for none.
   */
  @Nullable
  default Collection<Class<? extends M>> getSoftDependencies() {
    return null;
  }

  /**
   * Get a collection of {@link Module} classes that must be loaded first.
   *
   * <p>If any fail to load, the factory should operate as normal.
   *
   * @return The factory's weak dependencies or {@code null} for none.
   */
  @Nullable
  default Collection<Class<? extends M>> getWeakDependencies() {
    return null;
  }
}
