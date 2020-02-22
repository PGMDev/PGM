package tc.oc.util;

import com.google.common.collect.Iterables;
import java.util.Random;

public class RandomUtils {

  public static int safeNextInt(Random random, int i) {
    return i <= 0 ? 0 : random.nextInt(i);
  }

  public static <T> T element(Random random, Iterable<? extends T> collection) {
    return Iterables.get(collection, safeNextInt(random, Iterables.size(collection)));
  }
}
