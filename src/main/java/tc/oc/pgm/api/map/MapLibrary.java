package tc.oc.pgm.api.map;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

/** A library of {@link MapInfo}s. */
public interface MapLibrary {

  /**
   * Get the {@link MapInfo} given its id or name.
   *
   * @param idOrName The id or approximate name of a {@link MapInfo}.
   * @return The best matching {@link MapInfo} or {@code null} if not found.
   */
  @Nullable
  MapInfo getMap(String idOrName);

  /**
   * Get all {@link MapInfo}s, without any duplicate ids or names.
   *
   * @return A collection of {@link MapInfo}s.
   */
  Iterator<MapInfo> getMaps();

  /**
   * Refresh existing and discover new {@link MapInfo}s.
   *
   * @param reset Whether to forcibly bypass caches and discover all {@link MapSource}s.
   * @return A future when at all maps have attempted to build.
   */
  CompletableFuture<?> loadNewMaps(boolean reset);

  /**
   * Load a {@link MapContext} that has been discovered previously.
   *
   * @param id The exact id of the map.
   * @return A {@link MapContext} or {@code null} if not be found.
   */
  CompletableFuture<MapContext> loadExistingMap(String id);
}
