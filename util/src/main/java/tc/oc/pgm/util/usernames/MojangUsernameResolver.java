package tc.oc.pgm.util.usernames;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class MojangUsernameResolver implements UsernameResolver {
  private static final Gson GSON = new Gson();
  private static final long WAIT_TIME = 6; // Mojang api allows 10 requests a minute
  private static String userAgent = "PGM";
  private static Instant nextExecution = Instant.now();
  private static ExecutorService executor = Executors.newSingleThreadExecutor();
  private static long currentWaitTime = 6;
  protected final Map<UUID, CompletableFuture<UsernameResponse>> futures =
      new ConcurrentHashMap<>();

  static {
    try {
      final Plugin plugin = BukkitUtils.getPlugin();
      if (plugin != null) {
        userAgent = plugin.getDescription().getFullName() + Bukkit.getName();
      }
    } catch (Throwable t) {
      // No-op, just to be safe in-case agent cannot be found
    }
  }

  private CompletableFuture<UsernameResponse> resolveInternal(UUID uuid) {
    CompletableFuture<UsernameResponse> completableFuture = new CompletableFuture<>();
    completableFuture.whenComplete((o, t) -> futures.remove(uuid));
    executor.submit(() -> {
      Instant now = Instant.now();

      if (now.isBefore(nextExecution)) {
        try {
          Thread.sleep(Duration.between(now, nextExecution));
        } catch (InterruptedException e) {
          // Ignore
        }
      }

      String name = resolveSync(uuid);
      if (name != null) {
        currentWaitTime = WAIT_TIME;
      }
      nextExecution = Instant.now().plus(Duration.ofSeconds(currentWaitTime));

      Bukkit.getLogger().log(Level.FINE, "Resolved " + uuid + " as " + name);

      completableFuture.complete(UsernameResponse.of(name, MojangUsernameResolver.class));
    });
    return completableFuture;
  }

  @Override
  public CompletableFuture<UsernameResponse> resolve(UUID uuid) {
    return futures.computeIfAbsent(uuid, this::resolveInternal);
  }

  private static String resolveSync(UUID uuid) {
    try {
      final HttpURLConnection url = (HttpURLConnection) new URL(
              "https://api.minecraftservices.com/minecraft/profile/lookup/" + assertNotNull(uuid))
          .openConnection();
      url.setRequestMethod("GET");
      url.setRequestProperty("User-Agent", userAgent);
      url.setRequestProperty("Accept", "application/json");
      url.setInstanceFollowRedirects(true);
      url.setConnectTimeout(10000);
      url.setReadTimeout(10000);

      try (final BufferedReader br =
          new BufferedReader(new InputStreamReader(url.getInputStream(), StandardCharsets.UTF_8))) {
        return GSON.fromJson(br, JsonObject.class).get("name").getAsString();
      }
    } catch (Throwable t) {
      currentWaitTime *= 2;
      Bukkit.getLogger()
          .log(
              Level.WARNING,
              "Could not resolve username for " + uuid + " retrying in " + currentWaitTime
                  + " seconds!",
              t);
    }
    return null;
  }

  @Override
  public void startBatch() {
    // TODO: Refactor
  }

  @Override
  public CompletableFuture<Void> endBatch() {
    // TODO: Refactor
    return new CompletableFuture<>();
  }
}
