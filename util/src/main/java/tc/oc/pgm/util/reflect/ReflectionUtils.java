package tc.oc.pgm.util.reflect;

import java.lang.reflect.Field;
import javax.annotation.Nullable;

public final class ReflectionUtils {
  private ReflectionUtils() {}

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
}
