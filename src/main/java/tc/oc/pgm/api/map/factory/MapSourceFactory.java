package tc.oc.pgm.api.map.factory;

import java.util.Iterator;
import tc.oc.pgm.api.map.MapSource;
import tc.oc.pgm.api.map.exception.MapNotFoundException;

/** A thread-safe factory for creating {@link MapSource}s. */
public interface MapSourceFactory {

  /**
   * Get a lazily-populated iterator that returns "fresh" {@link MapSource}s.
   *
   * <p>Should include existing sources where {@link MapSource#checkForUpdates()} is true as well as
   * new ones that have not been discovered yet.
   *
   * @return An iterator of "fresh" {@link MapSource}s.
   * @throws MapNotFoundException If there is an error discovering new {@link MapSource}s.
   */
  Iterator<MapSource> loadSources() throws MapNotFoundException;

  /** Reset any caches so when {@link #loadSources()} is next called, it discovers all sources. */
  void reset();
}
