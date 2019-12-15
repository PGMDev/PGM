package tc.oc.pgm.map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;

public class NameCacheUtil {

  private static final Gson GSON = new Gson();

  private static List<UUID> unresolved = new ArrayList<UUID>();
  private static HashMap<UUID, CachedPlayer> cache = new HashMap<UUID, CachedPlayer>();

  public static boolean isUUIDCached(UUID uuid) {
    return cache.containsKey(uuid);
  }

  public static CachedPlayer getCachedPlayer(UUID uuid) {
    return cache.get(uuid);
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
              for (int i = 0; i < unresolved.size(); i++) {
                try {
                  UUID uuid = unresolved.get(i);
                  cache.put(
                      uuid,
                      new CachedPlayer(
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
    JsonWriter writer =
        new JsonWriter(new FileWriter(new File(PGM.get().getDataFolder(), "uuidcache.json")));
    writer.beginArray();
    for (Entry<UUID, CachedPlayer> entry : cache.entrySet()) {
      writer.beginObject();
      writer.name("uuid").value(entry.getKey().toString());
      writer.name("name").value(entry.getValue().getName());
      writer.name("timestamp").value(entry.getValue().getTimestamp());
      writer.endObject();
    }
    writer.endArray();
    writer.close();
  }

  public static void readCacheFromDisk() throws IOException {
    BufferedReader bufferedReader =
        new BufferedReader(new FileReader(new File(PGM.get().getDataFolder(), "uuidcache.json")));

    JsonArray array = GSON.fromJson(bufferedReader, JsonArray.class);
    for (int i = 0; i < array.size(); i++) {
      JsonObject obj = array.get(i).getAsJsonObject();
      CachedPlayer player =
          new CachedPlayer(
              UUID.fromString(obj.get("uuid").getAsString()),
              obj.get("name").getAsString(),
              obj.get("timestamp").getAsLong());
      // If the player's name has been fetched within the last seven days
      if (System.currentTimeMillis() - player.getTimestamp() < 604800000L) {
        cache.put(player.getUUID(), player);
      } else {
        unresolved.add(player.getUUID());
      }
    }
  }
}
