package tc.oc.pgm.util.usernames;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/**
 * Utility to resolve Minecraft usernames using Mojang's API
 *
 * @link <a href="https://minecraft.wiki/w/Mojang_API">Mojang API on minecraft.wiki</a>
 */
@NullMarked
public class MojangUsernameResolver extends AbstractBatchingUsernameResolver {
  private static final Gson GSON = new Gson();
  private static final int MAX_SEQUENTIAL_FAILURES = 5;
  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
  private static String userAgent = "PGM";

  // 700ms to stay below the 200 requests per 2 minutes limit
  private static final long REQUEST_INTERVAL_MS = 700;
  private static final long MAX_BACKOFF_MS = 64_000;
  private static long nextRequestAt = 0;
  private static long backoffMs = REQUEST_INTERVAL_MS;

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

  @Override
  protected Executor getExecutor() {
    return EXECUTOR;
  }

  @Override
  protected void process(UUID uuid, CompletableFuture<UsernameResponse> future) {
    String name = null;
    try {
      name = resolveSync(uuid);
    } catch (Throwable t) {
      Bukkit.getLogger()
          .log(Level.WARNING, LOG_PREFIX + "Could not resolve username for " + uuid, t);
    } finally {
      future.complete(UsernameResponse.of(name, MojangUsernameResolver.class));
    }
  }

  @Override
  protected void process(List<UUID> uuids) {
    final Map<UUID, Throwable> errors = new LinkedHashMap<>();

    final Instant now = Instant.now();

    int fails = 0;
    boolean stopped = false;
    for (UUID id : uuids) {
      if (stopped) {
        complete(id, UsernameResponse.empty());
        continue;
      }

      String name = null;
      try {
        name = resolveSync(id);
        fails = 0;
      } catch (Throwable t) {
        errors.put(id, t);
        if (++fails >= MAX_SEQUENTIAL_FAILURES
            || t instanceof UnknownHostException
            || t instanceof NoRouteToHostException) {
          stopped = true;
        }
      } finally {
        complete(id, UsernameResponse.of(name, now, MojangUsernameResolver.class));
      }
    }

    if (!errors.isEmpty()) {
      Bukkit.getLogger()
          .log(
              Level.WARNING,
              LOG_PREFIX + "Could not resolve " + errors.size() + " usernames",
              errors.values().iterator().next());
    }
  }

  @Nullable
  private static String resolveSync(UUID uuid) throws IOException, URISyntaxException {
    final long now = System.currentTimeMillis();
    if (now < nextRequestAt) {
      try {
        Thread.sleep(nextRequestAt - now);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    final HttpURLConnection conn = (HttpURLConnection)
        new URI("https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid)
            .toURL()
            .openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("User-Agent", userAgent);
    conn.setRequestProperty("Accept", "application/json");
    conn.setInstanceFollowRedirects(true);
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(10000);

    final int status;
    try {
      status = conn.getResponseCode();
    } catch (IOException e) {
      throw backoff(e);
    }
    nextRequestAt = System.currentTimeMillis() + REQUEST_INTERVAL_MS;

    if (status == HttpURLConnection.HTTP_OK) {
      backoffMs = REQUEST_INTERVAL_MS;
      try (final BufferedReader br = new BufferedReader(
          new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        return GSON.fromJson(br, JsonObject.class).get("name").getAsString();
      }
    } else if (status == HttpURLConnection.HTTP_NOT_FOUND) {
      // Player does not exist
      backoffMs = REQUEST_INTERVAL_MS;
      return null;
    } else {
      throw backoff(new IOException("Unexpected HTTP " + status));
    }
  }

  private static IOException backoff(IOException e) {
    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
    nextRequestAt = System.currentTimeMillis() + backoffMs;
    return new IOException(e.getMessage() + ", backing off for " + backoffMs + "ms", e);
  }
}
