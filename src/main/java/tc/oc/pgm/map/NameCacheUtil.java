package tc.oc.pgm.map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.util.UUID;
import org.bukkit.Bukkit;
import tc.oc.pgm.api.PGM;

public class NameCacheUtil {

  private static HashMap<UUID, String> cachedNames = new HashMap<UUID, String>();

  public static boolean isUUIDCached(UUID uuid) {
    return cachedNames.containsKey(uuid) && cachedNames.get(uuid) != null;
  }

  public static String getCachedName(UUID uuid) {
    return cachedNames.get(uuid);
  }

  public static void addUUID(UUID uuid) {
    if (!cachedNames.containsKey(uuid)) {
      cachedNames.put(uuid, null);
    }
  }

  private static String resolve(UUID uuid) throws IOException {
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
    return new Gson().fromJson(response.toString(), JsonObject.class).get("username").getAsString();
  }

  public static void resolveAll() {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            PGM.get(),
            () -> {
              for (UUID uuid : cachedNames.keySet()) {
                if (cachedNames.get(uuid) == null) {
                  try {
                    cachedNames.put(uuid, NameCacheUtil.resolve(uuid));
                  } catch (IOException ignored) {
                  }
                }
              }
              try {
                writeCacheToDisk();
              } catch (IOException ignored) {
              }
            });
  }

  public static void writeCacheToDisk() throws IOException {
    try (FileWriter out = new FileWriter(new File(PGM.get().getDataFolder(), "uuidcache"))) {
      for (UUID uuid : cachedNames.keySet()) {
        out.write(uuid.toString() + ":" + cachedNames.get(uuid) + "\n");
      }
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  public static void readCacheFromDisk() throws IOException {
    ArrayList<String> data = new ArrayList<String>();
    BufferedReader in =
        new BufferedReader(new FileReader(new File(PGM.get().getDataFolder(), "uuidcache")));
    String line;
    while ((line = in.readLine()) != null) {
      data.add(line);
    }
    in.close();

    for (String s : data) {
      String[] split = s.split(":", 2);
      cachedNames.put(UUID.fromString(split[0]), split[1]);
    }
  }
}
