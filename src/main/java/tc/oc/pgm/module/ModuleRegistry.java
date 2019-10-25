package tc.oc.pgm.module;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.map.*;
import tc.oc.pgm.match.FixtureMatchModuleFactory;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.match.MatchModuleFactory;

/**
 * Registry of static {@link MapModule}s and {@link MatchModule}s, which are created once for each
 * {@link PGMMap} and {@link Match} respectively. The modules are created using the factory provided
 * at registration time. Dynamically created {@link MatchModule}s (i.e. those created by {@link
 * MapModule}s at parse time) are *not* registered here.
 */
public class ModuleRegistry {

  private final Logger logger;
  private final Map<ModuleInfo, MatchModuleFactory<?>> matchModuleFactories = new LinkedHashMap<>();
  private final Map<ModuleInfo, MapModuleFactory<?>> moduleFactories = new LinkedHashMap<>();

  /** Creates a ModuleFactory instance with a specified logger. */
  public ModuleRegistry(Plugin plugin) {
    this.logger = plugin.getLogger();
  }

  public Logger getLogger() {
    return this.logger;
  }

  /**
   * Register the given {@link MapModule} subclass to be created for each map, using the given
   * factory.
   */
  public <T extends MapModule> void register(Class<T> klass, MapModuleFactory<T> factory)
      throws Throwable {
    moduleFactories.put(ModuleInfo.get(klass), factory);
    if (factory instanceof Registerable) {
      ((Registerable) factory).register(this, logger);
    }
  }

  /**
   * Register the given {@link MatchModule} subclass to be created for each match, using the given
   * factory.
   */
  public <T extends MatchModule> void register(Class<T> klass, MatchModuleFactory<T> factory)
      throws Throwable {
    matchModuleFactories.put(ModuleInfo.get(klass), factory);
    if (factory instanceof Registerable) {
      ((Registerable) factory).register(this, logger);
    }
  }

  /**
   * Register the given {@link MapModule} subclass to be created for each map, using a generic
   * factory. The factory will expect the module to have a public no-argument constructor.
   */
  public <T extends MapModule> void registerFixtureModule(Class<T> klass) throws Throwable {
    register(klass, new FixtureModuleFactory<>(klass));
  }

  /**
   * Register the given {@link MatchModule} subclass to be created for each match, using a generic
   * factory. The factory will expect the module to have a public constructor that takes a single
   * {@link Match} parameter.
   */
  public <T extends MatchModule> void registerFixtureMatchModule(Class<T> klass) throws Throwable {
    register(klass, new FixtureMatchModuleFactory<>(klass));
  }

  /**
   * Convenience method to call {@link #registerFixtureModule} or {@link
   * #registerFixtureMatchModule} as appropriate for the given class (unfortunately, this method
   * cannot be overloaded because the overloads would have the same erasure).
   */
  public void registerFixture(Class<?> klass) throws Throwable {
    if (MapModule.class.isAssignableFrom(klass)) {
      registerFixtureModule(klass.asSubclass(MapModule.class));
    } else if (MatchModule.class.isAssignableFrom(klass)) {
      registerFixtureMatchModule(klass.asSubclass(MatchModule.class));
    } else {
      throw new IllegalArgumentException("Invalid module type " + klass.getName());
    }
  }

  public <T extends MapModule> void registerStatic(Class<T> klass) throws Throwable {
    register(klass, new StaticMethodMapModuleFactory<>(klass));
  }

  /**
   * Gets a set of modules this ModuleFactory knows about.
   *
   * @return Set of ModuleInfo instances
   */
  public Set<ModuleInfo> getModules() {
    return moduleFactories.keySet();
  }

  public Set<ModuleInfo> getMatchModules() {
    return matchModuleFactories.keySet();
  }

  public MapModuleFactory<?> getModuleFactory(ModuleInfo info) {
    return moduleFactories.get(info);
  }

  public MatchModuleFactory<?> getMatchModuleFactory(ModuleInfo info) {
    return matchModuleFactories.get(info);
  }
}
