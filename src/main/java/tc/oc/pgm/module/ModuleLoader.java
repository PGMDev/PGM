package tc.oc.pgm.module;

import java.util.*;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import tc.oc.util.logging.ClassLogger;

/**
 * Common base for classes that load a dependency tree of modules. The module type can be anything,
 * but {@link ModuleInfo}s are always used to identify them.
 */
public abstract class ModuleLoader<T> {
  protected final Logger logger;

  private final Set<ModuleInfo> tried = new HashSet<>(); // Modules that have attempted to load
  private final Set<ModuleInfo> failed =
      new HashSet<>(); // Modules that threw an exception while loading
  private final Map<ModuleInfo, T> loaded =
      new LinkedHashMap<>(); // Modules that loaded successfully
  private final List<ModuleLoadException> errors = new ArrayList<>();

  protected ModuleLoader(Logger parentLogger) {
    this.logger = new ClassLogger(parentLogger, getClass());
  }

  /** Return all modules in dependency order */
  public Collection<T> getModules() {
    return this.loaded.values();
  }

  public T getModule(ModuleInfo info) {
    return loaded.get(info);
  }

  public <M extends T> M getModule(Class<M> klass) {
    return klass.cast(getModule(ModuleInfo.get(klass)));
  }

  public boolean hasModule(ModuleInfo info) {
    return this.getModule(info) != null;
  }

  public boolean hasModule(Class<? extends T> klass) {
    return this.getModule(klass) != null;
  }

  /**
   * Add an already loaded module to the set of loaded modules. This module can satisfy dependencies
   * and no attempt will be made to load it again.
   */
  public void addModule(ModuleInfo info, T module) {
    tried.add(info);
    loaded.put(info, module);
  }

  /**
   * Add an already loaded module to the set of loaded modules. This module can satisfy dependencies
   * and no attempt will be made to load it again.
   */
  public void addModule(T module) {
    addModule(ModuleInfo.get(module.getClass()), module);
  }

  public List<ModuleLoadException> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public void addError(ModuleLoadException e) {
    errors.add(e);
  }

  public void addErrors(Collection<? extends ModuleLoadException> errors) {
    this.errors.addAll(errors);
  }

  /**
   * Load and return the given module. This is the first attempt to load said module, and all of its
   * depencies have already loaded successfully.
   *
   * @return a fully loaded module, or null if the module declines to load
   * @throws ModuleLoadException in exceptional failure cases
   */
  protected abstract T loadModule(ModuleInfo info) throws ModuleLoadException;

  /**
   * Load all of the given modules in dependency order. If failFast is true, return immediately
   * after any loading exception. If failFast is false, continue loading other modules that do not
   * depend on the failed module. In either case, this method returns true only if there were no
   * errors.
   */
  public boolean loadAll(Collection<ModuleInfo> modules, boolean failFast) {
    for (ModuleInfo info : modules) {
      try {
        this.loadWithDependencies(info, null);
      } catch (ModuleLoadException e) {
        // Exception was already recorded at the source, so just move to the next module
        if (failFast) return false;
      }
    }
    return !hasErrors();
  }

  /**
   * Try to load all dependencies for the given module
   *
   * @return true if the module should continue to load, false if it should decline to load
   * @throws ModuleLoadException if a dependency threw an excepton while loading, or a hard
   *     dependency declined to load
   */
  public boolean loadDependencies(ModuleInfo info) throws ModuleLoadException {
    for (ModuleInfo require : info.getRequires()) {
      // Hard dependencies cannot return false, they either load or throw
      this.loadWithDependencies(require, info);
    }

    for (ModuleInfo depend : info.getDepends()) {
      // If a soft dependency declines, then we also decline
      if (!this.loadWithDependencies(depend, null)) {
        return false;
      }
    }

    for (ModuleInfo follow : info.getFollows()) {
      // These have to try loading before us, but we don't care about the result
      this.loadWithDependencies(follow, null);
    }

    return true;
  }

  /**
   * Try to load the given module, after loading all of its dependencies.
   *
   * <p>Any exception thrown while loading will propagate up the dependency tree and terminate the
   * entire chain, thus there will be at most one exception for each top-level module loaded.
   *
   * @return true if the module loaded, false if it declined to load
   * @throws ModuleLoadException if the module or one of its dependencies threw an exception while
   *     loading, or if requiredBy was given and the module declined to load.
   */
  public boolean loadWithDependencies(ModuleInfo info, @Nullable ModuleInfo requiredBy)
      throws ModuleLoadException {
    if (this.hasModule(info)) {
      return true;
    } else if (this.failed.contains(info)) {
      throw new ModuleLoadException(info);
    } else if (this.tried.contains(info)) {
      return false;
    }

    this.tried.add(info);

    if (!this.loadDependencies(info)) {
      return false;
    }

    try {
      T newModule = loadModule(info);
      if (newModule != null) {
        addModule(info, newModule);
        return true;
      }

      if (requiredBy != null) {
        throw new ModuleLoadException(
            info,
            info.getModuleClass().getSimpleName()
                + " failed to load, required by "
                + requiredBy.getModuleClass().getSimpleName());
      }

      return false;
    } catch (ModuleLoadException e) {
      e.fillInModule(info);
      this.failed.add(info);
      addError(e);
      throw e;
    } catch (Throwable t) {
      this.failed.add(info);
      ModuleLoadException e =
          new ModuleLoadException(
              info,
              "Unhandled "
                  + t.getClass().getName()
                  + " while parsing module '"
                  + info.getName()
                  + "': "
                  + t.getMessage(),
              t);
      addError(e);
      throw e;
    }
  }
}
