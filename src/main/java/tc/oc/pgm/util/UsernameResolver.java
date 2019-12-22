package tc.oc.pgm.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;

/**
 * Utility to resolve Minecraft usernames from an external API.
 *
 * @link https://github.com/Electroid/mojang-api
 */
public class UsernameResolver {

  private static final Map<UUID, SoftReference<Consumer<String>>> QUEUE = new ConcurrentHashMap<>();
  private static final Gson GSON = new Gson();

  /**
   * Resolve all remaining usernames on an asynchronous thread.
   *
   * @see #resolve(UUID, Consumer)
   */
  public static void resolveAll() {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            PGM.get(),
            () -> {
              final Map<UUID, SoftReference<Consumer<String>>> queue = ImmutableMap.copyOf(QUEUE);
              final List<UUID> failed = new LinkedList<>();
              Throwable throwable = null;

              for (UUID uuid : queue.keySet()) {
                try {
                  final String username = resolveNow(uuid);
                  final Consumer<String> callback = queue.get(uuid).get();

                  // If the callback still exists, run it on another thread to avoid a dead lock
                  if (callback != null) {
                    Bukkit.getScheduler()
                        .runTaskAsynchronously(PGM.get(), () -> callback.accept(username));
                  }
                } catch (Throwable t) {
                  failed.add(uuid);
                  throwable = t;
                } finally {
                  QUEUE.remove(uuid);
                }
              }

              if (!failed.isEmpty()) {
                PGM.get()
                    .getMapLogger()
                    .log(
                        Level.WARNING,
                        "Failed to lookup " + failed.size() + " usernames: " + failed,
                        throwable);
              }
            });
  }

  /**
   * Queue a username resolve with an asynchronous callback.
   *
   * @param uuid A {@link UUID} to resolve.
   * @param callback A callback to run after the username is resolved.
   */
  public static void resolve(UUID uuid, Consumer<String> callback) {
    QUEUE.put(
        uuid,
        new SoftReference<>(
            callback)); // Use soft reference to avoid potential leakage during queue
  }

  private static String resolveNow(UUID uuid) throws IOException {
    HttpURLConnection con =
        (HttpURLConnection)
            new URL("https://api.ashcon.app/mojang/v2/user/" + uuid.toString()).openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "Bukkit (" + Bukkit.getMotd() + ")");
    con.setRequestProperty("Accept", "application/json");
    con.setInstanceFollowRedirects(true);
    con.setConnectTimeout(5000);
    con.setReadTimeout(5000);
    BufferedReader br =
        new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      response.append(line.trim());
    }
    br.close();
    return GSON.fromJson(response.toString(), JsonObject.class).get("username").getAsString();
  }
}
