package tc.oc.pgm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.identity.Identities;
import tc.oc.named.NameRenderer;
import tc.oc.pgm.Config;
import tc.oc.pgm.Config.Flairs.Flair;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.tablist.PlayerTabEntry;
import tc.oc.tablist.TabManager;

public class FlairUtils {

  private static Map<UUID, String> flairCache = new HashMap<UUID, String>();
  private static MatchManager matchManagerInstance;
  private static NameRenderer nameRendererInstance;
  private static TabManager tabManagerInstance;

  public static void updateFlair(UUID uuid) {
    final MatchPlayer matchPlayer = matchManagerInstance.getPlayer(uuid);
    matchPlayer
        .getBukkit()
        .setDisplayName(getFlairedName(Bukkit.getPlayer(uuid), matchPlayer.getParty()));
    nameRendererInstance.invalidateCache(Identities.current(matchPlayer.getBukkit()));
    final PlayerTabEntry tabEntry =
        (PlayerTabEntry) tabManagerInstance.getPlayerEntryOrNull(matchPlayer.getBukkit());
    if (tabEntry != null) {
      tabEntry.invalidate();
      tabEntry.refresh();
    }
  }

  public static String getFlairedName(Player player, Party party) {
    return getFlairs(player)
        + (party == null ? ChatColor.RESET : party.getColor())
        + player.getName()
        + ChatColor.WHITE;
  }

  public static String getFlairs(Player player) {
    return Config.Flairs.configMode() ? getConfigFlairs(player) : getAPIFlairs(player);
  }

  private static String getConfigFlairs(Player player) {
    StringBuilder flair = new StringBuilder();
    for (Entry<String, Flair> entry : Config.Flairs.getFlairs().entrySet()) {
      if (player.hasPermission("pgm.flair." + entry.getKey())) {
        flair.append(entry.getValue().toString());
      }
    }
    return flair.toString();
  }

  private static String getAPIFlairs(Player player) {
    return flairCache.containsKey(player.getUniqueId()) ? flairCache.get(player.getUniqueId()) : "";
  }

  public static void setFlair(UUID uuid, String flair) {
    flairCache.put(uuid, flair);
    updateFlair(uuid);
  }

  @Nullable
  public static String getFlairString(UUID uuid) {
    return flairCache.get(uuid);
  }

  public static void removeFlair(UUID uuid) {
    flairCache.remove(uuid);
  }

  public static void setMatchManager(MatchManager instance) {
    matchManagerInstance = instance;
  }

  public static void setNameRenderer(NameRenderer instance) {
    nameRendererInstance = instance;
  }

  public static void setTabManager(TabManager instance) {
    tabManagerInstance = instance;
  }
}
