package tc.oc.pgm.api;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import tc.oc.pgm.api.map.MapActivity;
import tc.oc.pgm.api.player.Username;
import tc.oc.pgm.api.setting.Settings;

/**
 * A fast, persistent datastore that provides synchronous or asynchronous responses. Implementing
 * classes should override the synchronous methods and assume they are not executing on the main
 * thread.
 */
public interface Datastore {

  /**
   * Get the username for a given player {@link UUID} synchronously.
   *
   * @param uuid The {@link UUID} of a player.
   * @return A {@link Username}.
   */
  Username getUsername(UUID uuid);

  /**
   * Get the username for a given player {@link UUID} asynchronously.
   *
   * @param uuid The {@link UUID} of a player.
   * @param usernameCallback A callback that consumes a {@link Username}.
   */
  default void getUsername(UUID uuid, Consumer<Username> usernameCallback) {
    PGM.get()
        .getAsyncExecutor()
        .schedule(
            () -> {
              Username username = getUsername(uuid);
              PGM.get()
                  .getExecutor()
                  .schedule(() -> usernameCallback.accept(username), 0, TimeUnit.SECONDS);
            },
            0,
            TimeUnit.SECONDS);
  }

  /**
   * Get the settings for a given player {@link UUID} synchronously.
   *
   * @param uuid The {@link UUID} of a player.
   * @return A {@link Settings}.
   */
  Settings getSettings(UUID uuid);

  /**
   * Get the settings for a given player {@link UUID} asynchronously.
   *
   * @param uuid The {@link UUID} of a player.
   * @param settingsCallback A callback that consumes {@link Settings}.
   */
  default void getSettings(UUID uuid, Consumer<Settings> settingsCallback) {
    PGM.get()
        .getAsyncExecutor()
        .schedule(
            () -> {
              Settings settings = getSettings(uuid);
              PGM.get()
                  .getExecutor()
                  .schedule(() -> settingsCallback.accept(settings), 0, TimeUnit.SECONDS);
            },
            0,
            TimeUnit.SECONDS);
  }

  /**
   * Get the activity related to a defined map pool synchronously.
   *
   * @param poolName The name of a defined map pool.
   * @return A {@link MapActivity}.
   */
  MapActivity getMapActivity(String poolName);

  /**
   * Get the activity related to a defined map pool asynchronously.
   *
   * @param poolName The name of a defined map pool.
   * @param mapActivityCallback A callback that consumes {@link MapActivity}.
   */
  default void getMapActivity(String poolName, Consumer<MapActivity> mapActivityCallback) {
    PGM.get()
        .getAsyncExecutor()
        .schedule(
            () -> {
              MapActivity mapActivity = getMapActivity(poolName);
              PGM.get()
                  .getExecutor()
                  .schedule(() -> mapActivityCallback.accept(mapActivity), 0, TimeUnit.SECONDS);
            },
            0,
            TimeUnit.SECONDS);
  }
}
