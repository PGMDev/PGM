package tc.oc.pgm.util;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/**
 * Utility to resolve Minecraft usernames from an external API.
 *
 * @link https://github.com/Electroid/mojang-api
 */
public final class UsernameResolver {
  private UsernameResolver() {}

  private static final Gson GSON = new Gson();
  private static final Semaphore LOCK = new Semaphore(1);
  private static final Map<UUID, Consumer<String>> QUEUE = new ConcurrentHashMap<>();
  private static final double MAX_SEQUENTIAL_FAILURES = 5;
  private static String userAgent = "PGM";

  static {
    try {
      final Plugin plugin = BukkitUtils.getPlugin();
      if (plugin != null) {
        userAgent = plugin.getDescription().getFullName();
      }
    } catch (Throwable t) {
      // No-op, just to be safe in-case agent cannot be found
    }
  }

  /**
   * Queue all remaining username resolves on an asynchronous thread.
   *
   * @see #resolve(UUID, Consumer)
   */
  public static void resolveAll() {
    if (!LOCK.tryAcquire()) return;
    CompletableFuture.runAsync(UsernameResolver::resolveAllSync);
  }

  /**
   * Queue a username resolve with an asynchronous callback.
   *
   * @param id A {@link UUID} to resolve.
   * @param callback A callback to run after the username is resolved.
   */
  public static void resolve(UUID id, @Nullable Consumer<String> callback) {
    final Consumer<String> existing = QUEUE.get(assertNotNull(id));
    if (callback == null) callback = i -> {};

    // If a callback already exists, chain the new one after the existing one
    if (existing != null && callback != existing) {
      callback = existing.andThen(callback);
    }

    QUEUE.put(id, callback);
  }

  private static void resolveAllSync() {
    LOCK.tryAcquire();

    final Set<UUID> queue = ImmutableSet.copyOf(QUEUE.keySet());
    final Map<UUID, Throwable> errors = new LinkedHashMap<>();

    int fails = 0;
    for (UUID id : queue) {
      String name = null;
      try {
        name = resolveSync(id);
        fails = 0;
      } catch (Throwable t) {
        errors.put(id, t);
        if (++fails > MAX_SEQUENTIAL_FAILURES
            || t instanceof UnknownHostException
            || t instanceof NoRouteToHostException) break;
      } finally {
        final Consumer<String> listener = QUEUE.remove(id);
        if (listener != null) {
          try {
            listener.accept(name);
          } catch (Throwable t) {
            // No-op
          }
        }
      }
    }

    if (!errors.isEmpty()) {
      Bukkit.getLogger()
          .log(
              Level.FINEST,
              "Could not resolve " + errors.size() + " usernames",
              errors.values().iterator().next());
    }

    LOCK.release();
  }

  private static String resolveSync(UUID id) throws IOException {
    final HttpURLConnection url =
        (HttpURLConnection)
            new URL("https://api.ashcon.app/mojang/v2/user/" + assertNotNull(id).toString())
                .openConnection();
    url.setRequestMethod("GET");
    url.setRequestProperty("User-Agent", userAgent);
    url.setRequestProperty("Accept", "application/json");
    url.setInstanceFollowRedirects(true);
    url.setConnectTimeout(10000);
    url.setReadTimeout(10000);

    final StringBuilder response = new StringBuilder();
    try (final BufferedReader br =
        new BufferedReader(new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        response.append(line.trim());
      }
    }

    return GSON.fromJson(response.toString(), JsonObject.class).get("username").getAsString();
  }
}
