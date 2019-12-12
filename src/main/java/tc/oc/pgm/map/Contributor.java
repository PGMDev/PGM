package tc.oc.pgm.map;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.identity.Identity;
import tc.oc.identity.RealIdentity;
import tc.oc.named.NameStyle;
import tc.oc.named.Named;

/**
 * A contributor to a {@link PGMMap}. Can have either or both of a UUID and arbitrary String name.
 * If a UUID is present, it is used to lookup a username when the map loads. The fallback name is
 * only used if the lookup fails, or no UUID is provided (this could be used to credit somebody
 * without a Minecraft account, like mom or Jesus).
 */
public class Contributor implements Named {

  private static HashMap<UUID, String> cachedNames = new HashMap<UUID, String>();

  protected final @Nullable UUID uuid;
  protected final @Nullable String fallbackName;
  protected final @Nullable String contribution;

  protected @Nullable UUID playerId;

  /** Creates a contributor with a name and a contribution. */
  public Contributor(
      @Nullable UUID uuid, @Nullable String fallbackName, @Nullable String contribution) {
    this.uuid = uuid;
    this.fallbackName = fallbackName;
    this.contribution = contribution;

    checkArgument(uuid != null || fallbackName != null);
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public @Nullable UUID getUuid() {
    return uuid;
  }

  /** Gets the name of this contributor. */
  public @Nullable String getName() {
    return isUUIDCached(this.uuid) ? getCachedName(this.uuid) : this.fallbackName;
  }

  public @Nullable UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public @Nullable Identity getIdentity() {
    return playerId == null
        ? null
        : new RealIdentity(getPlayerId(), Bukkit.getPlayer(getPlayerId()).getName());
  }

  @Override
  public Component getStyledName(NameStyle style) {
    return playerId != null
        ? new PersonalizedPlayer(getIdentity(), style)
        : new PersonalizedText(fallbackName);
  }

  /** @return true only if a username is available */
  public boolean hasName() {
    return this.playerId != null || this.fallbackName != null;
  }

  public boolean needsLookup() {
    return this.uuid != null && this.playerId == null;
  }

  /** Indicates whether or not this contributor has a specific contribution. */
  public boolean hasContribution() {
    return this.contribution != null;
  }

  /** Gets this contributor's contribution or null if none exists. */
  public @Nullable String getContribution() {
    return this.contribution;
  }

  public static List<Contributor> filterNamed(List<Contributor> contributors) {
    List<Contributor> resolved = new ArrayList<>();
    for (Contributor contributor : contributors) {
      if (contributor.hasName()) {
        resolved.add(contributor);
      }
    }
    return resolved;
  }

  public static boolean isUUIDCached(UUID uuid) {
    return cachedNames.containsKey(uuid);
  }

  public static String getCachedName(UUID uuid) {
    return cachedNames.get(uuid);
  }

  public static void cacheName(UUID uuid, String name) {
    cachedNames.put(uuid, name);
  }

  public static void resolve(UUID uuid) throws IOException {
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
    String line = null;
    while ((line = br.readLine()) != null) {
      response.append(line.trim());
    }
    br.close();
    String n =
        new Gson().fromJson(response.toString(), JsonObject.class).get("username").getAsString();
    cacheName(uuid, n);
  }
}
