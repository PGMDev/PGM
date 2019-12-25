package tc.oc.pgm.prefix;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.identity.Identities;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.tablist.PlayerTabEntry;

public class PrefixRegistryImpl implements PrefixRegistry, Listener {

  private PrefixProvider prefixProvider;

  @Override
  @EventHandler
  public void onPrefixChange(PrefixChangeEvent event) {
    if (event.getUUID() == null) {
      return;
    }
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(event.getUUID());
    if (matchPlayer == null
        || matchPlayer.getBukkit() == null
        || matchPlayer.getBukkit().isOnline()) {
      return;
    }
    matchPlayer
        .getBukkit()
        .setDisplayName(getPrefixedName(Bukkit.getPlayer(event.getUUID()), matchPlayer.getParty()));
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
    return getPrefix(player.getUniqueId())
        + (party == null ? ChatColor.RESET : party.getColor())
        + player.getName()
        + ChatColor.WHITE;
  }

  @Override
  public String getPrefix(UUID uuid) {
    return prefixProvider != null ? prefixProvider.getPrefix(uuid) : "";
  }

  @Override
  public void setPrefixProvider(PrefixProvider prefixProvider) {
    this.prefixProvider = prefixProvider;
  }

  @Nullable
  @Override
  public PrefixProvider getPrefixProvider() {
    return prefixProvider;
  }
}
