package tc.oc.pgm.match;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import javax.annotation.Nullable;
import tc.oc.pgm.module.ModuleLoadException;
import tc.oc.util.reflect.ReflectionUtils;

/**
 * Creates a {@link MatchModule} unconditionally for every match. The module must have a public
 * constructor that accepts one {@link Match} parameter.
 */
public class FixtureMatchModuleFactory<T extends MatchModule> implements MatchModuleFactory<T> {

  private final Constructor<T> constructor;

  public FixtureMatchModuleFactory(Class<T> matchModuleClass) {
    if (Modifier.isAbstract(matchModuleClass.getModifiers())) {
      throw new IllegalArgumentException(matchModuleClass.getName() + " is abstract");
    }

    try {
      this.constructor = matchModuleClass.getConstructor(Match.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          "Missing constructor "
              + matchModuleClass.getSimpleName()
              + "(Match) in class "
              + matchModuleClass.getName());
    }

    ReflectionUtils.assertPublicThrows(constructor, ModuleLoadException.class);
  }

  @Override
  public @Nullable T createMatchModule(Match match) throws ModuleLoadException {
    try {
      return constructor.newInstance(match);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof ModuleLoadException) {
        throw (ModuleLoadException) e.getCause();
      } else {
        // Already verified that it cannot throw any other checked exceptions
        throw (RuntimeException) e.getCause();
      }
    } catch (InstantiationException | IllegalAccessException e) {
      // This should be pretty much impossible due to the checks in the constructor
      throw new IllegalStateException(e);
    }
  }
}
