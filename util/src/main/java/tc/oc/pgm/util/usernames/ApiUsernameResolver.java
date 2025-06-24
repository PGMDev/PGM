package tc.oc.pgm.util.usernames;

import static tc.oc.pgm.util.Assert.assertNotNull;

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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/**
 * Utility to resolve Minecraft usernames from an external API.
 *
 * @link https://github.com/Electroid/mojang-api
 */
public final class ApiUsernameResolver extends AbstractBatchingUsernameResolver {
  private static final Gson GSON = new Gson();
  private static final int MAX_SEQUENTIAL_FAILURES = 5;
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

  @Override
  protected void process(UUID uuid, CompletableFuture<UsernameResponse> future) {
    String name = null;
    try {
      name = resolveSync(uuid);
    } catch (Throwable t) {
      Bukkit.getLogger().log(Level.WARNING, "Could not resolve username for " + uuid, t);
    } finally {
      future.complete(UsernameResponse.of(name, ApiUsernameResolver.class));
    }
  }

  @Override
  protected void process(List<UUID> uuids) {
    final Map<UUID, Throwable> errors = new LinkedHashMap<>();

    Instant now = Instant.now();

    int fails = 0;
    boolean stopped = false;
    for (UUID id : uuids) {
      // Even if there's an issue, we need to complete the futures.
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
        if (++fails > MAX_SEQUENTIAL_FAILURES
            || t instanceof UnknownHostException
            || t instanceof NoRouteToHostException) {
          stopped = true;
        }
      } finally {
        complete(id, UsernameResponse.of(name, now, ApiUsernameResolver.class));
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

  private static String resolveSync(UUID id) throws IOException {
    final HttpURLConnection url =
        (HttpURLConnection)
            new URL("https://api.ashcon.app/mojang/v2/user/" + assertNotNull(id)).openConnection();
    url.setRequestMethod("GET");
    url.setRequestProperty("User-Agent", userAgent);
    url.setRequestProperty("Accept", "application/json");
    url.setInstanceFollowRedirects(true);
    url.setConnectTimeout(10000);
    url.setReadTimeout(10000);

    try (final BufferedReader br =
        new BufferedReader(new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
      return GSON.fromJson(br, JsonObject.class).get("username").getAsString();
    }
  }
}
