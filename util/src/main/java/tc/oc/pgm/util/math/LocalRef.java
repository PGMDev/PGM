package tc.oc.pgm.util.math;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Address of a local variable, resolved at parse time: {@code depth} frames up from the current
 * {@link LocalFrame}, then index {@code slot} in that frame.
 */
public record LocalRef(int depth, int slot) {
  // Maps are parsed on async threads
  private static final Map<LocalRef, LocalRef> CACHE = new ConcurrentHashMap<>();

  public static LocalRef of(int depth, int slot) {
    var key = new LocalRef(depth, slot);
    var cached = CACHE.putIfAbsent(key, key);
    return cached == null ? key : cached;
  }
}
