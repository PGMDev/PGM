package tc.oc.pgm.flair;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.identity.Identities;
import tc.oc.pgm.Config;
import tc.oc.pgm.Config.Flairs.Flair;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.tablist.PlayerTabEntry;

public class FlairRegistryImpl implements FlairRegistry {

  private final Map<UUID, String> flairCache = new HashMap<UUID, String>();

  @Override
  public void updateDisplayName(UUID uuid) {
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(uuid);
    matchPlayer
        .getBukkit()
        .setDisplayName(getFlairedName(Bukkit.getPlayer(uuid), matchPlayer.getParty()));
    PGM.get().getNameRenderer().invalidateCache(Identities.current(matchPlayer.getBukkit()));
    final PlayerTabEntry tabEntry =
        (PlayerTabEntry)
            PGM.get().getMatchTabManager().getPlayerEntryOrNull(matchPlayer.getBukkit());
    if (tabEntry != null) {
      tabEntry.invalidate();
      tabEntry.refresh();
    }
  }

  @Override
  public String getFlairedName(Player player, Party party) {
    return getFlairs(player)
        + (party == null ? ChatColor.RESET : party.getColor())
        + player.getName()
        + ChatColor.WHITE;
  }

  @Override
  public void setFlair(UUID uuid, String flair) {
    flairCache.put(uuid, flair);
    updateDisplayName(uuid);
  }

  @Override
  @Nullable
  public String getFlair(UUID uuid) {
    return flairCache.get(uuid);
  }

  @Override
  public void removeFlair(UUID uuid) {
    flairCache.remove(uuid);
  }

  private String getFlairs(Player player) {
    return Config.Flairs.enabled()
        ? getConfigFlairs(player)
        : (getAPIFlairs(player) != null ? getAPIFlairs(player) : "");
  }

  private String getConfigFlairs(Player player) {
    StringBuilder flair = new StringBuilder();
    for (Entry<String, Flair> entry : Config.Flairs.getFlairs().entrySet()) {
      if (player.hasPermission("pgm.flair." + entry.getKey())) {
        flair.append(entry.getValue().toString());
      }
    }
    return flair.toString();
  }

  @Nullable
  private String getAPIFlairs(Player player) {
    return flairCache.containsKey(player.getUniqueId())
        ? flairCache.get(player.getUniqueId())
        : null;
  }
}
