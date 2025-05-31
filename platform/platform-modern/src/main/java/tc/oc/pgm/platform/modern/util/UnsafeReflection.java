package tc.oc.pgm.platform.modern.util;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

@SuppressWarnings({"deprecation", "unchecked"})
public class UnsafeReflection {
  private static final Unsafe UNSAFE;

  static {
    try {
      final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <T> T getFieldUnsafe(Object base, String name) {
    try {
      long offset = UNSAFE.objectFieldOffset(base.getClass().getDeclaredField(name));
      //noinspection unchecked
      return (T) UNSAFE.getObject(base, offset);
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  public static <T> void setStaticFieldUnsafe(Field field, T value) {
    try {
      var staticBase = UNSAFE.staticFieldBase(field);
      var offset = UNSAFE.staticFieldOffset(field);
      UNSAFE.putObject(staticBase, offset, value);
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(t);
    }
  }
}
