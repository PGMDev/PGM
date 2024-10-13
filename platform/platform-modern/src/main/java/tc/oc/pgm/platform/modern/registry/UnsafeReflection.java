package tc.oc.pgm.platform.modern.registry;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

@SuppressWarnings("deprecation")
class UnsafeReflection {
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
}
