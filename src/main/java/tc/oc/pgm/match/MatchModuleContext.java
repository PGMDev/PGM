package tc.oc.pgm.match;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import tc.oc.pgm.features.MatchFeatureContext;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.util.collection.IterableUtils;

public class MatchModuleContext {
  protected final Map<Class<? extends MatchModule>, MatchModule> matchModules =
      new LinkedHashMap<>();
  protected final MatchFeatureContext matchFeatureContext;

  public MatchModuleContext(MatchFeatureContext matchFeatureContext) {
    this.matchFeatureContext = matchFeatureContext;
  }

  public MatchFeatureContext getFeatureContext() {
    return this.matchFeatureContext;
  }

  public boolean load(MatchModule module) throws ModuleLoadException {
    Class<? extends MatchModule> key = module.getClass();
    if (matchModules.containsKey(key)) {
      throw new ModuleLoadException("Tried to create multiple " + key.getSimpleName());
    }

    matchModules.put(key, module);

    try {
      if (!module.shouldLoad()) {
        matchModules.remove(key);
        return false;
      }

      module.load();
      return true;
    } catch (Throwable e) {
      matchModules.remove(key);
      throw e;
    }
  }

  public Collection<MatchModule> getAll() {
    return ImmutableList.copyOf(this.matchModules.values());
  }

  @SuppressWarnings("unchecked")
  public <T extends MatchModule> T getMatchModule(Class<T> matchModuleClass) {
    return (T) this.matchModules.get(matchModuleClass);
  }

  @SuppressWarnings("unchecked")
  public <T extends MatchModule> Iterable<T> getMatchModulesOfType(
      final Class<?> parentMatchModuleClass) {
    return IterableUtils.transfilter(
        matchModules.keySet(),
        new Function<Class<? extends MatchModule>, T>() {
          @Nullable
          @Override
          public T apply(@Nullable Class<? extends MatchModule> sub) {
            return parentMatchModuleClass.isAssignableFrom(sub) ? (T) matchModules.get(sub) : null;
          }
        });
  }
}
