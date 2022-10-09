package tc.oc.pgm.util.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jetbrains.annotations.Nullable;

public final class ReflectionUtils {
  private ReflectionUtils() {}

  public static Class<?> getClassFromName(String className) {
    try {
      Class<?> clazz = ReflectionUtils.class.getClassLoader().loadClass(className);
      return clazz;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Method getMethod(Class<?> parent, String name) {
    try {
      Method method = parent.getDeclaredMethod(name);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static void callMethod(Class<?> parent, String name, Object object) {
    try {
      Method method = getMethod(parent, name);
      method.invoke(object);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static void callMethod(Method method, Object object) {
    try {
      method.invoke(object);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static Field getField(String className, String fieldName) {
    return getField(getClassFromName(className), fieldName);
  }

  public static Field getField(Class<?> parent, String name) {
    try {
      Field field = parent.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T readField(Class<?> parent, @Nullable Object obj, Class<T> type, String name) {
    try {
      return type.cast(readField(obj, parent.getDeclaredField(name)));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object readField(@Nullable Object obj, Field field) {
    final boolean wasAccessible = field.isAccessible();
    try {
      field.setAccessible(true);
      return field.get(obj);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } finally {
      field.setAccessible(wasAccessible);
    }
  }

  public static void setField(Class<?> parent, Object base, Object value, String fieldName) {
    try {
      setField(base, value, parent.getDeclaredField(fieldName));
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setField(Object base, Object value, Field field) {
    field.setAccessible(true);
    try {
      field.set(base, value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
