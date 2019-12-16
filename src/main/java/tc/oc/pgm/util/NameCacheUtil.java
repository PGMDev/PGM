package tc.oc.pgm.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;

public class NameCacheUtil {

  private static final Gson GSON = new Gson();
  private static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;

  private static Set<UUID> unresolved = new HashSet<UUID>();
  private static Map<UUID, NameCacheEntry> cache = new HashMap<UUID, NameCacheEntry>();

  public static boolean isUUIDCached(UUID uuid) {
    return cache.containsKey(uuid);
  }

  @Nullable
  public static String getCachedName(UUID uuid) {
    NameCacheEntry entry = cache.get(uuid);
    return entry == null ? null : entry.name;
  }

  public static void addUUID(UUID uuid) {
    if (!cache.containsKey(uuid)) {
      unresolved.add(uuid);
    }
  }

  private static String resolveName(UUID uuid) throws IOException {
    HttpURLConnection con =
        (HttpURLConnection)
            new URL("https://api.ashcon.app/mojang/v2/user/" + uuid.toString()).openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("Accept", "application/json");
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

  public static void resolveAll() {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            PGM.get(),
            () -> {
              for (UUID uuid : unresolved) {
                try {
                  cache.put(
                      uuid,
                      new NameCacheEntry(
                          uuid, NameCacheUtil.resolveName(uuid), System.currentTimeMillis()));
                } catch (IOException ignored) {
                }
              }
              unresolved.clear();
              try {
                writeCacheToDisk();
              } catch (IOException ignored) {
              }
            });
  }

  public static void writeCacheToDisk() throws IOException {
    try (FileWriter writer =
        new FileWriter(new File(PGM.get().getDataFolder(), "uuidcache.json"))) {
      GSON.toJson(cache, writer);
    }
  }

  public static void readCacheFromDisk() throws IOException {
    long oneWeekAgo = System.currentTimeMillis() - ONE_WEEK;
    try (FileReader reader =
        new FileReader(new File(PGM.get().getDataFolder(), "uuidcache.json"))) {
      cache = GSON.fromJson(reader, new TypeToken<HashMap<UUID, NameCacheEntry>>() {}.getType());
    }
    cache.values().stream()
        .filter(entry -> entry.timestamp > oneWeekAgo)
        .forEach(ce -> unresolved.add(ce.uuid));
  }

  private static class NameCacheEntry {

    public final UUID uuid;
    public final long timestamp;
    public String name;

    public NameCacheEntry(UUID uuid, String name, long timestamp) {
      this.uuid = uuid;
      this.name = name;
      this.timestamp = timestamp;
    }
  }
}
