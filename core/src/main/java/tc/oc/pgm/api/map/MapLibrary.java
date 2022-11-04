package tc.oc.pgm.api.map;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.includes.MapIncludeProcessor;

/** A library of {@link MapInfo}s and {@link MapContext}s. */
public interface MapLibrary {

  /**
   * Get the {@link MapInfo} given its id or name.
   *
   * @param idOrName The id or an approximate name of a map.
   * @return The best matching {@link MapInfo} or {@code null} if not found.
   */
  @Nullable
  MapInfo getMap(String idOrName);

  /**
   * Get all {@link MapInfo}s matching the query.
   *
   * @return A stream of {@link MapInfo}s.
   */
  Stream<MapInfo> getMaps(String query);

  /**
   * Get all {@link MapInfo}s, without any duplicate ids or names.
   *
   * @return A collection of {@link MapInfo}s.
   */
  Iterator<MapInfo> getMaps();

  /**
   * Get the number of {@link MapInfo}s.
   *
   * @return The number of {@link MapInfo}s.
   */
  long getSize();

  /**
   * Reload existing and discover new {@link MapContext}s.
   *
   * @param reset Whether to forcibly reset all {@link MapSource}s.
   * @return A future when at all maps have attempted to load.
   */
  CompletableFuture<?> loadNewMaps(boolean reset);

  /**
   * Load a {@link MapContext} that has been previously discovered.
   *
   * @param id The exact id of the map.
   * @return A {@link MapContext}.
   */
  CompletableFuture<MapContext> loadExistingMap(String id);

  /**
   * Get the {@link MapIncludeProcessor}.
   *
   * @return A {@link MapIncludeProcessor}
   */
  MapIncludeProcessor getIncludeProcessor();
}
