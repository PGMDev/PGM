package tc.oc.pgm.module;

import java.util.*;
import javax.annotation.Nullable;

/** Class that parses runtime annotations for a module and presents it in an easy-to-use package. */
public class ModuleInfo {
  private static final Map<Class<?>, ModuleInfo> byClass = new HashMap<>();

  /**
   * Get the {@link ModuleInfo} for the given class, or create one if it doesn't exist. If the class
   * is annotated with a {@link ModuleDescription}, that will be used to fill in the info fields.
   * Otherwise, the name will be derived from the class name, and all dependencies will be empty.
   *
   * <p>{@link ModuleInfo}s are stored in a global registry. This method never returns null and will
   * never create multiple info objects for the same class.
   */
  public static ModuleInfo get(Class<?> klass) {
    ModuleInfo info = byClass.get(klass);
    return info != null ? info : new ModuleInfo(klass);
  }

  private final Class<?> moduleClass;
  private final @Nullable ModuleDescription desc;

  private ModuleInfo(Class<?> moduleClass, ModuleDescription desc) {
    this.moduleClass = moduleClass;
    this.desc = desc;
    byClass.put(moduleClass, this);
  }

  private ModuleInfo(Class<?> moduleClass) {
    this(moduleClass, moduleClass.getAnnotation(ModuleDescription.class));
  }

  public Class<?> getModuleClass() {
    return moduleClass;
  }

  /**
   * Gets the static name of this module.
   *
   * @return Name of the module.
   */
  public String getName() {
    return desc != null ? desc.name() : moduleClass.getSimpleName();
  }

  public Set<ModuleInfo> getDepends() {
    return desc == null ? Collections.<ModuleInfo>emptySet() : getModuleInfos(desc.depends());
  }

  public Set<ModuleInfo> getRequires() {
    return desc == null ? Collections.<ModuleInfo>emptySet() : getModuleInfos(desc.requires());
  }

  public Set<ModuleInfo> getFollows() {
    return desc == null ? Collections.<ModuleInfo>emptySet() : getModuleInfos(desc.follows());
  }

  private Set<ModuleInfo> getModuleInfos(Class<?>... modules) {
    Set<ModuleInfo> infos = new HashSet<>(modules.length);

    for (Class<?> module : modules) {
      infos.add(get(module));
    }

    return infos;
  }
}
