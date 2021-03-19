package tc.oc.pgm.util.reflect;

import java.lang.reflect.Field;
import javax.annotation.Nullable;

public final class ReflectionUtils {
  private ReflectionUtils() {}

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
