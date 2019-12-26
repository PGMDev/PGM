package tc.oc.pgm.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import tc.oc.identity.IdentityProvider;
import tc.oc.named.NameRenderer;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.development.MapErrorTracker;
import tc.oc.pgm.map.MapLibrary;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.prefix.PrefixRegistry;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.util.SemanticVersion;

/** PvP Game Manager (aka. PGM), the global {@link Plugin} to manage PvP games. */
public interface PGM extends Plugin {

  /**
   * Get a datastore that persists between matches and server restarts.
   *
   * @return A persistent, synchronous datastore.
   */
  Datastore getDatastore();

  /**
   * Get a cached datastore that persists between matches and server restarts.
   *
   * @return {@link #getDatastore()} wrapped in an in-memory cache.
   */
  Datastore getDatastoreCache();

  /**
   * Get the specific manager that loads and unloads {@link Match}s.
   *
   * @return The {@link MatchManager}.
   */
  MatchManager getMatchManager();

  MatchTabManager getMatchTabManager();

  /**
   * Get the specific manager that parses and loads {@link PGMMap}s.
   *
   * @return The {@link MapLibrary}.
   */
  MapLibrary getMapLibrary();

  /**
   * Get the latest {@link SemanticVersion} that {@link PGMMap}s can support.
   *
   * @return The latest {@link PGMMap} {@link SemanticVersion}.
   */
  SemanticVersion getMapProtoSupported();

  /**
   * Get the shared {@link Logger} for all {@link PGMMap}s, separate from the server {@link Logger}.
   *
   * @return The {@link PGMMap} {@link Logger}.
   */
  Logger getMapLogger();

  /**
   * Get the error tracker that allows for filtering and clearing of {@link PGMMap} errors.
   *
   * @return The {@link MapErrorTracker}.
   */
  MapErrorTracker getMapErrorTracker();

  PrefixRegistry getPrefixRegistry();

  @Deprecated
  IdentityProvider getIdentityProvider();

  @Deprecated
  NameRenderer getNameRenderer();

  AtomicReference<PGM> GLOBAL = new AtomicReference<>(null);

  static PGM set(PGM pgm) {
    try {
      get();
      throw new IllegalArgumentException("PGM was already initialized!");
    } catch (IllegalStateException e) {
      GLOBAL.set(checkNotNull(pgm, "PGM cannot be null!"));
    }
    return get();
  }

  static PGM get() {
    final PGM pgm = GLOBAL.get();
    if (pgm == null) {
      throw new IllegalStateException("PGM is not yet enabled!");
    }
    return pgm;
  }
}
