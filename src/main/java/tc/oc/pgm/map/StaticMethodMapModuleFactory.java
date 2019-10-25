package tc.oc.pgm.map;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.jdom2.Document;
import tc.oc.pgm.module.ModuleRegistry;
import tc.oc.pgm.module.Registerable;
import tc.oc.util.reflect.ReflectionUtils;
import tc.oc.xml.InvalidXMLException;

/** Delegates module creation/registration to static methods on the module class */
public class StaticMethodMapModuleFactory<T extends MapModule>
    implements MapModuleFactory<T>, Registerable {

  private final Class<T> moduleClass;
  private final Method registerMethod;
  private final Method parseMethod;

  public StaticMethodMapModuleFactory(Class<T> moduleClass) {
    this.moduleClass = moduleClass;

    Method method;
    try {
      method = moduleClass.getMethod("register", ModuleRegistry.class, Logger.class);
    } catch (NoSuchMethodException e) {
      method = null;
    }
    this.registerMethod = method;

    if (registerMethod != null) {
      ReflectionUtils.assertPublic(registerMethod);
    }

    try {
      method = moduleClass.getMethod("parse", MapModuleContext.class, Logger.class, Document.class);
    } catch (NoSuchMethodException e) {
      method = null;
    }
    this.parseMethod = method;

    if (parseMethod != null) {
      ReflectionUtils.assertPublicThrows(parseMethod, InvalidXMLException.class);
    }
  }

  @Override
  public void register(ModuleRegistry context, Logger logger) throws Throwable {
    if (registerMethod == null) return;

    try {
      registerMethod.invoke(null, context, logger);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Override
  public @Nullable T parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    if (parseMethod == null) return null;

    try {
      return moduleClass.cast(parseMethod.invoke(null, context, logger, doc));
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw InvalidXMLException.coerce(e);
    }
  }
}
