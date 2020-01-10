package tc.oc.pgm.api.module;

import java.util.Collection;
import javax.annotation.Nullable;

public interface ModuleContext<T extends Module> extends Module {

  Collection<T> getModules();

  @Nullable
  <M extends T> M getModule(Class<? extends M> key);

  default <M extends T> M needModule(Class<? extends M> key) {
    final M module = getModule(key);
    if (module == null) {
      throw new IllegalStateException("Required module " + key + " was not found");
    }
    return module;
  }

  default boolean hasModule(Class<? extends T> key) {
    return getModule(key) != null;
  }
}
