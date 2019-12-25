package tc.oc.pgm.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.identity.Identities;
import tc.oc.pgm.Config.Prefixes;
import tc.oc.pgm.Config.Prefixes.Prefix;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.tablist.PlayerTabEntry;

public class PrefixRegistryImpl implements PrefixRegistry {

  private final Map<UUID, String> prefixCache = new HashMap<UUID, String>();

  @Override
  public void updateDisplayName(UUID uuid) {
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(uuid);
    matchPlayer
        .getBukkit()
        .setDisplayName(getPrefixedName(Bukkit.getPlayer(uuid), matchPlayer.getParty()));
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
  public String getPrefixedName(Player player, Party party) {
    return getPrefixes(player)
        + (party == null ? ChatColor.RESET : party.getColor())
        + player.getName()
        + ChatColor.WHITE;
  }

  @Override
  public void setPrefix(UUID uuid, String prefix) {
    prefixCache.put(uuid, prefix);
    updateDisplayName(uuid);
  }

  @Override
  @Nullable
  public String getPrefix(UUID uuid) {
    return prefixCache.get(uuid);
  }

  @Override
  public void removePlayer(UUID uuid) {
    prefixCache.remove(uuid);
  }

  private String getPrefixes(Player player) {
    return Prefixes.enabled()
        ? getConfigPrefixes(player)
        : (getAPIPrefixes(player) != null ? getAPIPrefixes(player) : "");
  }

  private String getConfigPrefixes(Player player) {
    StringBuilder prefix = new StringBuilder();
    for (Entry<String, Prefix> entry : Prefixes.getPrefixes().entrySet()) {
      if (player.hasPermission("pgm.flair." + entry.getKey())) {
        prefix.append(entry.getValue().toString());
      }
    }
    return prefix.toString();
  }

  @Nullable
  private String getAPIPrefixes(Player player) {
    return prefixCache.containsKey(player.getUniqueId())
        ? prefixCache.get(player.getUniqueId())
        : null;
  }
}
