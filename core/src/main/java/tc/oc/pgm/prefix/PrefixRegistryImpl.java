package tc.oc.pgm.prefix;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.Config.Prefixes;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.PrefixChangeEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.prefix.PrefixProvider;
import tc.oc.pgm.api.prefix.PrefixRegistry;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.util.bukkit.tablist.PlayerTabEntry;

public class PrefixRegistryImpl implements PrefixRegistry, Listener {

  private PrefixProvider prefixProvider;

  public PrefixRegistryImpl() {
    this.prefixProvider = Prefixes.enabled() ? new ConfigPrefixProvider() : null;
  }

  @EventHandler
  public void onJoinMatch(PlayerJoinMatchEvent event) {
    Player player = event.getPlayer().getBukkit();
    player.setDisplayName(getPrefixedName(player, event.getNewParty()));
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    Player player = event.getPlayer().getBukkit();
    player.setDisplayName(getPrefixedName(player, event.getNewParty()));
  }

  @Override
  @EventHandler
  public void onPrefixChange(PrefixChangeEvent event) {
    if (event.getUUID() == null) {
      return;
    }
    final Player player = Bukkit.getPlayer(event.getUUID());
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) {
      return;
    }
    player.setDisplayName(getPrefixedName(player, matchPlayer.getParty()));
    final MatchTabManager tabManager = PGM.get().getMatchTabManager();
    if (tabManager != null) {
      final PlayerTabEntry tabEntry = (PlayerTabEntry) tabManager.getPlayerEntryOrNull(player);
      if (tabEntry != null) {
        tabEntry.invalidate();
        tabEntry.refresh();
      }
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
  public void removePlayer(UUID uuid) {
    if (prefixProvider != null) {
      prefixProvider.removePlayer(uuid);
    }
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
