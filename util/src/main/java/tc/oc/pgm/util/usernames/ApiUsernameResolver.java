package tc.oc.pgm.util.usernames;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** Utility to resolve Minecraft usernames from an external HTTP API. */
public class ApiUsernameResolver extends AbstractBatchingUsernameResolver {
  private static final int HTTP_OK = 200;
  private static final int HTTP_TOO_MANY_REQUESTS = 429;

  private static final Gson GSON = new Gson();
  private static final int MAX_SEQUENTIAL_FAILURES = 5;
  private static final int MIN_BACKOFF = 5_000;
  private static final int MAX_BACKOFF = 60_000;
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

  private final String logPrefix;
  private final String path;
  private final HttpClient httpClient;
  private final Executor executor;
  private final String[] jsonPath;

  private long backoffMs = MIN_BACKOFF;

  public ApiUsernameResolver(String name, String path, boolean singleThreaded, String jsonPath) {
    assertTrue(path.contains("{uuid}"));
    this.logPrefix = "[ApiUsernameResolver:" + name + "] ";
    this.path = path;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    this.executor =
        singleThreaded ? Executors.newSingleThreadExecutor() : AbstractUsernameResolver.EXECUTOR;
    this.jsonPath = jsonPath.split("\\.");
  }

  @Override
  protected Executor getExecutor() {
    return executor;
  }

  @Override
  protected String logPrefix() {
    return logPrefix;
  }

  @Override
  protected void process(UUID uuid, CompletableFuture<UsernameResponse> future) {
    String name = null;
    try {
      name = resolveSync(uuid, 5);
    } catch (Throwable t) {
      warn("Could not resolve username for " + uuid, t);
    } finally {
      future.complete(UsernameResponse.of(name, getClass()));
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
        name = resolveSync(id, 5);
        if (name != null) {
          backoffMs = backoff(0.95d, true);
          fails = 0;
        }
      } catch (Throwable t) {
        errors.put(id, t);
        if (++fails > MAX_SEQUENTIAL_FAILURES
            || t instanceof UnknownHostException
            || t instanceof NoRouteToHostException) {
          stopped = true;
          warn("Stopped resolving usernames", t);
        }
      } finally {
        complete(id, UsernameResponse.of(name, now, getClass()));
      }
    }

    if (!errors.isEmpty())
      warn(
          "Could not resolve " + errors.size() + " usernames",
          errors.values().iterator().next());
  }

  private String resolveSync(UUID id, int maxRetries) throws Exception {
    var response = httpClient.send(buildRequest(id), HttpResponse.BodyHandlers.ofString());

    return switch (response.statusCode()) {
      case HTTP_OK -> getUsername(GSON.fromJson(response.body(), JsonObject.class));
      case HTTP_TOO_MANY_REQUESTS -> {
        if (maxRetries > 1) {
          backoffMs = backoff(2d, true);
          var jitter = (Math.random() * 0.5d);
          Thread.sleep(backoff(1d + jitter, false));
          yield resolveSync(id, maxRetries - 1);
        }
        throw new IOException("Too many requests");
      }
      default -> null;
    };
  }

  private long backoff(double scaling, boolean clamp) {
    long backoff = (long) (backoffMs * scaling);
    if (clamp) backoff = Math.clamp(backoff, MIN_BACKOFF, MAX_BACKOFF);
    return backoff;
  }

  protected HttpRequest buildRequest(UUID uuid) throws Exception {
    return HttpRequest.newBuilder()
        .GET()
        .uri(new URI(path.replace("{uuid}", assertNotNull(uuid).toString())))
        .header("User-Agent", userAgent)
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(10))
        .build();
  }

  // Each API impl has to decide how to extract the username
  protected String getUsername(JsonObject response) {
    JsonElement curr = response;
    for (String s : jsonPath) {
      if (curr == null || !curr.isJsonObject()) return null;
      curr = curr.getAsJsonObject().get(s);
    }
    return curr.getAsString();
  }
}
