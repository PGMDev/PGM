package tc.oc.pgm.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.util.reflect.ReflectionUtils;
import tc.oc.xml.InvalidXMLException;

/**
 * Creates a {@link MapModule} unconditionally for every map. The module must have a public
 * no-argument constructor.
 */
public class FixtureModuleFactory<T extends MapModule> implements MapModuleFactory<T> {

  private final Constructor<T> constructor;

  public FixtureModuleFactory(Class<T> moduleClass) {
    if (Modifier.isAbstract(moduleClass.getModifiers())) {
      throw new IllegalArgumentException(moduleClass.getName() + " is abstract");
    }

    try {
      this.constructor = moduleClass.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(moduleClass.getName() + " has no default constructor");
    }

    ReflectionUtils.assertPublicThrows(constructor);
  }

  @Override
  public @Nullable T parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    try {
      return constructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw InvalidXMLException.coerce(e);
    }
  }
}
