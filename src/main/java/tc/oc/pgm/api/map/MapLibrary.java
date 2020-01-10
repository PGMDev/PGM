package tc.oc.pgm.api.map;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import tc.oc.util.SemanticVersion;

/** A library of {@link MapInfo}s and {@link MapContext}s. */
public interface MapLibrary {

  /**
   * Get the {@link MapContext} given its id or name.
   *
   * @param idOrName The id or approximate name of a {@link MapContext}.
   * @return The best matching {@link MapContext} or {@code null} if not found.
   */
  @Nullable
  MapContext getMap(String idOrName);

  /**
   * Get all {@link MapContext}s, without any duplicate ids or names.
   *
   * @return A collection of {@link MapContext}s.
   */
  Collection<MapContext> getMaps();

  /**
   * Refresh existing and discover new {@link MapContext}s.
   *
   * @param force Whether to forcibly bypass caches and discover all {@link MapSource}s.
   * @return A future when at least 1 map has been found.
   */
  CompletableFuture<Boolean> loadMaps(boolean force);

  /**
   * Get the latest {@link ProtoVersions} that is supported.
   *
   * @return The most recent {@link ProtoVersions}.
   */
  SemanticVersion getProto();
}
