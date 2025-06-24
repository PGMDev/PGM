package tc.oc.pgm.api;

import static tc.oc.pgm.util.Assert.assertNotNull;

import fr.minuskube.inv.InventoryManager;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapLibrary;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.channels.ChatManager;
import tc.oc.pgm.namedecorations.NameDecorationRegistry;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.util.listener.AfkTracker;

/** PvP Game Manager (aka. PGM), the global {@link Plugin} to manage PvP games. */
public interface PGM extends Plugin {

  Config getConfiguration();

  Logger getGameLogger();

  Datastore getDatastore();

  MatchManager getMatchManager();

  @Nullable
  MatchTabManager getMatchTabManager();

  MapLibrary getMapLibrary();

  MapOrder getMapOrder();

  NameDecorationRegistry getNameDecorationRegistry();

  ScheduledExecutorService getExecutor();

  ScheduledExecutorService getAsyncExecutor();

  InventoryManager getInventoryManager();

  AfkTracker getAfkTracker();

  ChatManager getChatManager();

  AtomicReference<PGM> GLOBAL = new AtomicReference<>(null);

  static PGM set(PGM pgm) {
    try {
      get();
      throw new IllegalArgumentException("PGM was already initialized!");
    } catch (IllegalStateException e) {
      GLOBAL.set(assertNotNull(pgm, "PGM cannot be null!"));
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
