package tc.oc.pgm.namedecorations;

import java.util.UUID;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.tablist.MatchTabManager;
import tc.oc.pgm.util.tablist.PlayerTabEntry;

public class NameDecorationRegistryImpl implements NameDecorationRegistry, Listener {

  private NameDecorationProvider provider;

  public NameDecorationRegistryImpl(@Nullable NameDecorationProvider provider) {
    this.provider = provider;
  }

  @EventHandler
  public void onJoinMatch(PlayerJoinMatchEvent event) {
    Player player = event.getPlayer().getBukkit();
    player.setDisplayName(getDecoratedName(player, event.getNewParty()));
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    Player player = event.getPlayer().getBukkit();
    player.setDisplayName(getDecoratedName(player, event.getNewParty()));
  }

  @EventHandler
  public void onNameDecorationChange(NameDecorationChangeEvent event) {
    refreshPlayer(event.getUUID());
  }

  @Override
  public String getDecoratedName(Player player, Party party) {
    return getPrefix(player.getUniqueId())
        + (party == null ? ChatColor.RESET : party.getColor())
        + player.getName()
        + getSuffix(player.getUniqueId())
        + ChatColor.WHITE;
  }

  public String getPrefix(UUID uuid) {
    return provider != null ? provider.getPrefix(uuid) : "";
  }

  public String getSuffix(UUID uuid) {
    return provider != null ? provider.getSuffix(uuid) : "";
  }

  @Override
  public void refreshPlayer(UUID uuid) {
    if (uuid == null) return;

    final Player player = Bukkit.getPlayer(uuid);
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;

    matchPlayer.getBukkit().setDisplayName(getDecoratedName(player, matchPlayer.getParty()));

    final MatchTabManager tabManager = PGM.get().getMatchTabManager();
    if (tabManager != null) {
      final PlayerTabEntry tabEntry = (PlayerTabEntry) tabManager.getPlayerEntryOrNull(player);
      if (tabEntry != null) tabEntry.invalidate();
    }
  }

  @Override
  public void setProvider(@Nullable NameDecorationProvider provider) {
    this.provider = provider;
  }

  @Nullable
  @Override
  public NameDecorationProvider getProvider() {
    return provider;
  }
}
