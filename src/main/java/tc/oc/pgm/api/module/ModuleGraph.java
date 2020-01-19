package tc.oc.pgm.api.module;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

/** A dependency graph for {@link Module}s and their {@link ModuleFactory}s. */
public abstract class ModuleGraph<M extends Module, F extends ModuleFactory<M>>
    implements ModuleContext<M> {

  private final AtomicBoolean loaded;
  private final Map<Class<? extends M>, F> factories;
  private Map<Class<? extends M>, M> modules;

  public ModuleGraph(Map factories) {
    this.loaded = new AtomicBoolean(true); // unloadAll will change to false
    this.factories = factories; // No copies since every graph would be duplicated
    unloadAll();
  }

  /**
   * Creates a {@link Module} from a {@link ModuleFactory}.
   *
   * @param factory A {@link ModuleFactory}.
   * @return A {@link Module} or {@code null} to silently skip.
   * @throws ModuleLoadException If there is an error creating the {@link Module}.
   */
  protected abstract @Nullable M createModule(F factory) throws ModuleLoadException;

  protected void loadAll() throws ModuleLoadException {
    if (!loaded.compareAndSet(false, true)) return;

    try {
      final Stack<ModuleLoadException> errors = new Stack<>();
      for (Class<? extends M> key : factories.keySet()) {
        load(key, null, errors);

        for (ModuleLoadException e : errors) {
          throw e;
        }
      }

      modules = Collections.unmodifiableMap(modules);
    } catch (ModuleLoadException e) {
      unloadAll();
      throw e;
    }
  }

  protected void unloadAll() {
    if (loaded.compareAndSet(true, false)) {
      modules = new HashMap<>(factories.size());
    }
  }

  protected void addFactory(Class<? extends M> key, F factory) {
    factories.put(key, factory);

    final Stack<ModuleLoadException> errors = new Stack<>();
    if (loaded.get()) {
      load(key, null, errors);
    }

    if (!errors.isEmpty()) {
      throw errors.get(0);
    }
  }

  protected F getFactory(Class<? extends M> key, @Nullable Class<? extends M> requiredBy)
      throws ModuleLoadException {
    if (checkNotNull(key) == requiredBy) {
      throw new ModuleLoadException(key, "Required itself (is there a circular dependency?)");
    }

    final F factory = factories.get(key);
    if (factory == null) {
      if (requiredBy == null) {
        throw new ModuleLoadException(
            key, "Required but not registered in " + getClass().getSimpleName());
      }
      throw new ModuleLoadException(
          key,
          "Required by "
              + requiredBy.getSimpleName()
              + " but not registered in "
              + getClass().getSimpleName());
    }

    return factory;
  }

  private boolean load(
      Class<? extends M> key,
      @Nullable Class<? extends M> requiredBy,
      Stack<ModuleLoadException> errors)
      throws ModuleLoadException {
    final F factory = getFactory(key, requiredBy);
    if (factory == null) {
      return false;
    }

    if (modules.containsKey(key)) {
      return true;
    }

    final Collection<Class<? extends M>> hardDependencies = factory.getHardDependencies();
    if (hardDependencies != null && !hardDependencies.isEmpty()) {
      for (Class<? extends M> hardDependency : hardDependencies) {
        if (!load(hardDependency, key, errors)) {
          throw new ModuleLoadException(
              key,
              hardDependency.getSimpleName() + " is a hard dependency that failed to load",
              errors.lastElement());
        }
      }
    }

    final Collection<Class<? extends M>> softDependencies = factory.getSoftDependencies();
    if (softDependencies != null && !softDependencies.isEmpty()) {
      for (Class<? extends M> softDependency : softDependencies) {
        try {
          if (!load(softDependency, key, errors)) {
            return false;
          }
        } catch (ModuleLoadException e) {
          errors.push(e);
          return false;
        }
      }
    }

    final Collection<Class<? extends M>> weakDependencies = factory.getWeakDependencies();
    if (weakDependencies != null && !weakDependencies.isEmpty()) {
      for (Class<? extends M> weakDependency : weakDependencies) {
        try {
          load(weakDependency, key, errors);
        } catch (ModuleLoadException e) {
          errors.push(e);
        }
      }
    }

    @Nullable M module;
    try {
      module = createModule(factory);
    } catch (ModuleLoadException e) {
      errors.add(e);
      throw e;
    }

    if (module == null) {
      return false;
    }

    modules.put((Class<? extends M>) module.getClass(), module);

    return true;
  }

  @Override
  public <N extends M> N getModule(Class<? extends N> key) {
    return (N) modules.get(key);
  }

  @Override
  public Collection<M> getModules() {
    return modules.values();
  }
}
