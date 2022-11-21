package tc.oc.pgm.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.event.player.PlayerSkinChangeEvent;

public class SkinCache implements Listener {

  private final Cache<UUID, Skin> offlineSkins =
      CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(3, TimeUnit.HOURS).build();
  private final Random random = new Random();

  private final Map<UUID, Skin> customSkins = Maps.newHashMap();

  // TODO: NEEDS WORK! Backup skins when 0 are online, prevent duplicates, etc
  private Skin getRandomSkin() {
    if (offlineSkins.size() == 0) {
      return Skin.EMPTY; // TODO: Warning, this may be bad for 1.16 clients...
    }
    List<Skin> skins = offlineSkins.asMap().values().stream().collect(Collectors.toList());
    return skins.get(random.nextInt(skins.size()));
  }

  private Skin getSkin(Player player) {
    if (customSkins.containsKey(player.getUniqueId())) {
      return customSkins.get(player.getUniqueId());
    }
    return getRandomSkin();
  }

  private boolean canUseSkin(Player player) {
    return !player.hasPermission(Permissions.STAFF)
        && !player.hasPermission(Permissions.PREMIUM); // TODO: add specific node too
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (canUseSkin(player)) {
      offlineSkins.put(player.getUniqueId(), player.getSkin());
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    offlineSkins.invalidate(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void refreshNamesOnLogin(PlayerJoinEvent event) {
    refreshPlayer(event.getPlayer());
  }

  // SPORTPAPER STUFF - TODO: Add alternative method and check if server is running SportPaper to
  // enable

  public void refreshAllViewers(Player player) {
    Bukkit.getOnlinePlayers().forEach(viewer -> refreshFakeName(player, viewer));
  }

  public void refreshPlayer(Player player) {
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;

    // Update displayname
    player.setDisplayName(
        PGM.get()
            .getNameDecorationRegistry()
            .getDecoratedName(player, matchPlayer.getParty().getColor()));

    // for all other online players, refresh their views
    refreshAllViewers(player);

    // Refresh the view of the player
    refreshSelfView(player);

    // Reset visibility
    matchPlayer.resetVisibility();
  }

  public void refreshSelfView(Player viewer) {
    Bukkit.getOnlinePlayers().forEach(other -> refreshFakeName(other, viewer));
  }

  // TODO: Figure out how to use without SPORTPAPER API
  public void refreshFakeName(Player player, Player viewer) {
    boolean nicked = Integration.getNick(player) != null;
    boolean areFriends = Integration.isFriend(player, viewer);
    boolean isViewerStaff = viewer.hasPermission(Permissions.STAFF);
    boolean isViewerAdmin = viewer.hasPermission(Permissions.ADMIN);
    boolean override = player.hasPermission(Permissions.ADMIN);

    boolean canSeeRealName =
        override ? isViewerAdmin : (isViewerStaff || player == viewer || areFriends);

    if (nicked && !canSeeRealName) {
      String nick = Integration.getNick(player);
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      String displayName =
          PGM.get()
              .getNameDecorationRegistry()
              .getDecoratedName(player, matchPlayer.getParty().getColor());
      player.setFakeDisplayName(viewer, displayName);
      player.setFakeNameAndSkin(viewer, nick, getSkin(player));
    } else {
      player.setFakeDisplayName(viewer, null);
      player.setFakeNameAndSkin(viewer, null, null);
    }
  }

  @EventHandler
  public void onSkinRefresh(PlayerSkinChangeEvent event) {
    if (event.getSkin() == null) {
      customSkins.remove(event.getPlayer().getUniqueId());
    }

    if (Integration.getNick(event.getPlayer()) != null) {
      if (event.getSkin() != null) {
        // Store custom skin for persistence
        customSkins.put(event.getPlayer().getUniqueId(), event.getSkin());
      }

      // Refresh skin for everyone online
      refreshPlayer(event.getPlayer());
    }
  }
}
