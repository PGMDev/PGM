package tc.oc.util.collection;

import java.util.Arrays;

public class ArrayUtils {
  private ArrayUtils() {}

  public static final int NOT_FOUND_INDEX = -1;

  public static <T> T fromEnd(T[] array, int index) {
    return array[array.length - 1 - index];
  }

  public static <T> int indexOf(T[] array, T value) {
    if (array == null) return NOT_FOUND_INDEX;

    if (value == null) {
      for (int i = 0; i < array.length; i++) {
        if (array[i] == null) return i;
      }
    } else {
      for (int i = 0; i < array.length; i++) {
        if (value.equals(array[i])) return i;
      }
    }

    return NOT_FOUND_INDEX;
  }

  public static <T> boolean contains(T[] array, T value) {
    return indexOf(array, value) != NOT_FOUND_INDEX;
  }

  public static <T> T[] append(T[] a, T... b) {
    if (b.length == 0) return a;
    if (a.length == 0) return b;

    T[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }
}
