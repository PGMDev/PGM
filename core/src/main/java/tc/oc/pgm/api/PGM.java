package tc.oc.pgm.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.prefix.PrefixRegistry;
import tc.oc.pgm.tablist.MatchTabManager;

/** PvP Game Manager (aka. PGM), the global {@link Plugin} to manage PvP games. */
public interface PGM extends Plugin {

  Logger getGameLogger();

  Datastore getDatastore();

  MatchManager getMatchManager();

  @Nullable
  MatchTabManager getMatchTabManager();

  MapLibrary getMapLibrary();

  MapOrder getMapOrder();

  PrefixRegistry getPrefixRegistry();

  ScheduledExecutorService getExecutor();

  ScheduledExecutorService getAsyncExecutor();

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
