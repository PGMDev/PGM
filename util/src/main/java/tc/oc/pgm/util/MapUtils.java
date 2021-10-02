package tc.oc.pgm.util;

import java.util.Map;
import java.util.function.Function;

public class MapUtils {
  /**
   * A version of {@link Map#computeIfAbsent(Object, Function)} that allows you to safely access the
   * map from inside the compute function. Some of the specialized implementations of the original
   * method in the JDK (e.g. the one in {@link java.util.HashMap}) can put the map in an illegal
   * state if you try to do that.
   */
  public static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<K, V> computer) {
    V value = map.get(key);
    if (value == null) {
      value = computer.apply(key);
      if (value != null) {
        map.put(key, value);
      }
    }
    return value;
  }
}
