package tc.oc.pgm.nick;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.nick.NickProvider;

public class NickRegistryImpl implements NickRegistry {

  private final MetadataValue METADATA_VALUE = new FixedMetadataValue(PGM.get(), this);

  private NickProvider provider;

  private final Logger logger;
  private final Random random;
  private final Cache<UUID, Skin> offlineSkins;

  public NickRegistryImpl(@Nullable NickProvider provider, Logger logger) {
    setProvider(provider);
    this.logger = logger;
    this.random = new Random();
    this.offlineSkins =
        CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(3, TimeUnit.HOURS).build();
  }

  @Override
  public NickProvider getProvider() {
    return provider;
  }

  @Override
  public void setProvider(@Nullable NickProvider provider) {
    this.provider = provider == null ? NickProvider.DEFAULT : provider;
  }

  @Override
  public Optional<String> getNick(UUID playerId) {
    return provider.getNick(playerId);
  }

  // TODO: NEEDS WORK! Backup skins when 0 are online, prevent duplicates, etc
  public Skin getRandomSkin() {
    if (offlineSkins.size() == 0) {
      return Skin.EMPTY; // TODO: Warning, this may be bad for 1.16 clients...
    }
    List<Skin> skins = offlineSkins.asMap().values().stream().collect(Collectors.toList());
    return skins.get(random.nextInt(skins.size()));
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
      logger.info("Cached regular player skin - " + player.getName());
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    player.setMetadata(METADATA_KEY, METADATA_VALUE);

    offlineSkins.invalidate(player.getUniqueId());
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
  }

  public void refreshSelfView(Player viewer) {
    Bukkit.getOnlinePlayers().forEach(other -> refreshFakeName(other, viewer));
  }

  // TODO: Figure out how to use without SPORTPAPER API
  public void refreshFakeName(Player player, Player viewer) {
    boolean nicked = getNick(player).isPresent();

    if (nicked && !viewer.hasPermission(Permissions.STAFF)) {
      String nick = getNick(player).get();
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player.getUniqueId());
      String displayName =
          PGM.get()
              .getNameDecorationRegistry()
              .getDecoratedName(player, matchPlayer.getParty().getColor());
      player.setFakeDisplayName(viewer, displayName);
      player.setFakeNameAndSkin(viewer, nick, getRandomSkin());
      logger.info("Nick: Skin and fakename set for " + player.getName() + " -> " + displayName);
    } else {
      player.setFakeDisplayName(viewer, null);
      player.setFakeNameAndSkin(viewer, null, null);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void refreshNamesOnLogin(PlayerJoinEvent event) {
    refreshPlayer(event.getPlayer());
    refreshSelfView(event.getPlayer());

    Player player = event.getPlayer();
    if (provider != null) {
      getNick(player)
          .ifPresent(
              nickname -> {
                String fakeName = player.getFakeDisplayName(Bukkit.getConsoleSender());
                logger.info(player.getName() + " has logged in disguised as " + fakeName);
              });
    }
  }
}
