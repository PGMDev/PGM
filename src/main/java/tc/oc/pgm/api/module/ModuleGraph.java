package tc.oc.pgm.api.module;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import tc.oc.pgm.api.module.exception.ModuleLoadException;

public abstract class ModuleGraph<M extends Module, F extends ModuleFactory> {

  private static final LoadingCache<ModuleFactory, Class<? extends Module>> KEYS =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<ModuleFactory, Class<? extends Module>>() {
                @Override
                public Class<? extends Module> load(ModuleFactory factory) {
                  return factory.getModuleClass();
                }
              });

  private final Map<Class<? extends M>, F> factories;

  public ModuleGraph(Map<Class<? extends M>, F> factories) {
    this.factories = factories; // No copies because every graph would have its own copy
  }

  protected abstract @Nullable M getModule(F factory) throws ModuleLoadException;

  private Class<? extends M> getKey(F factory) throws ModuleLoadException {
    try {
      return (Class<? extends M>) KEYS.get(factory);
    } catch (ExecutionException e) {
      throw new ModuleLoadException(
          "Could not extract module class from " + factory.getClass().getSimpleName());
    }
  }

  private F getFactory(Class<? extends M> key, @Nullable Class<? extends M> queuedBy)
      throws ModuleLoadException {
    final F factory = factories.get(key);

    if (factory == null) {
      if (queuedBy == null) {
        throw new ModuleLoadException(
            key, "Required but not registered in " + getClass().getSimpleName());
      }
      throw new ModuleLoadException(
          key,
          "Required by "
              + queuedBy.getSimpleName()
              + " but not registered in "
              + getClass().getSimpleName());
    }

    return factory;
  }

  private boolean load(
      F factory,
      Map<Class<? extends M>, Boolean> keys,
      List<M> modules,
      Stack<ModuleLoadException> errors)
      throws ModuleLoadException {
    final Class<? extends M> key = getKey(factory);

    if (keys.containsKey(key)) {
      return keys.getOrDefault(key, false);
    }

    final Collection<Class<? extends M>> hardDependencies = factory.getHardDependencies();
    if (hardDependencies != null && !hardDependencies.isEmpty()) {
      for (Class<? extends M> hardDependency : hardDependencies) {
        final F hardFactory = getFactory(hardDependency, key);

        if (!load(hardFactory, keys, modules, errors)) {
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
        final F softFactory = getFactory(softDependency, key);

        try {
          if (!load(softFactory, keys, modules, errors)) {
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
        final F weakFactory = getFactory(weakDependency, key);

        try {
          load(weakFactory, keys, modules, errors);
        } catch (ModuleLoadException e) {
          errors.push(e);
        }
      }
    }

    @Nullable M module;
    try {
      module = getModule(factory);
    } catch (ModuleLoadException e) {
      errors.add(e);
      throw e;
    }

    if (module == null) {
      return false;
    }

    return modules.add(module);
  }

  protected Collection<M> loadAll() throws ModuleLoadException {
    final Map<Class<? extends M>, Boolean> keys = new HashMap<>();
    final List<M> modules = new LinkedList<>();
    final Stack<ModuleLoadException> errors = new Stack<>();

    final Iterator<F> iterator = factories.values().iterator();
    while (iterator.hasNext()) {
      load(iterator.next(), keys, modules, errors);
    }

    for (ModuleLoadException e : errors) {
      throw e;
    }

    return modules;
  }
}
