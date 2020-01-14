package tc.oc.pgm.api.map.factory;

import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;

import java.util.Iterator;

/** A thread-safe factory for creating {@link MapSource}s. */
public interface MapSourceFactory {

  /**
   * Get a lazily-populated iterator that returns new {@link MapSource}s.
   *
   * <p>It is the responsibility of the callee to keep a strong reference to each {@link MapSource}
   * it wants to keep, otherwise it will be garbage collected.
   *
   * @return An iterator of new {@link MapSource}s.
   * @throws MapNotFoundException If there is an issue discovering.
   */
  Iterator<? extends MapSource> loadNewSources() throws MapNotFoundException;

  /** Reset any caches so when {@link #loadNewSources()} is next called, it discovers everything. */
  void reset();
}
