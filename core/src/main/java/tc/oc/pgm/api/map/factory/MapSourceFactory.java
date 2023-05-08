package tc.oc.pgm.api.map.factory;

import java.util.function.Consumer;
import java.util.stream.Stream;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapException;

/** A thread-safe factory for creating {@link MapSource}s. */
public interface MapSourceFactory {

  /**
   * Get a lazily-populated iterator that returns new {@link MapSource}s.
   *
   * <p>It is the responsibility of the callee to keep a strong reference to each {@link MapSource}
   * it wants to keep, otherwise it will be garbage collected.
   *
   * @param exceptionHandler place to send any exception when loading maps
   * @return An iterator of new {@link MapSource}s.
   */
  Stream<? extends MapSource> loadNewSources(Consumer<MapException> exceptionHandler);

  /**
   * Reset any caches so when {@link #loadNewSources(Consumer)} is next called, it discovers
   * everything.
   */
  void reset();
}
