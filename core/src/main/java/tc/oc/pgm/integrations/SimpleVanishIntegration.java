package tc.oc.pgm.integrations;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.VanishIntegration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;

public class SimpleVanishIntegration implements VanishIntegration, Listener {

  private static final String VANISH_KEY = "isVanished";
  private static final MetadataValue VANISH_VALUE = new FixedMetadataValue(PGM.get(), true);

  private final List<UUID> vanishedPlayers;
  private final MatchManager matchManager;

  private final Future<?>
      hotbarTask; // Task is run every second to ensure vanished players retain hotbar message
  private boolean hotbarFlash;

  public SimpleVanishIntegration(MatchManager matchManager, ScheduledExecutorService tasks) {
    this.vanishedPlayers = Lists.newArrayList();
    this.matchManager = matchManager;
    this.hotbarFlash = false;
    this.hotbarTask =
        tasks.scheduleAtFixedRate(
            () -> {
              getOnlineVanished().forEach(p -> sendHotbarVanish(p, hotbarFlash));
              hotbarFlash = !hotbarFlash; // Toggle boolean so we get a nice flashing effect
            },
            0,
            1,
            TimeUnit.SECONDS);

    // Register listener
    PGM.get().getServer().getPluginManager().registerEvents(this, PGM.get());
  }

  public void disable() {
    hotbarTask.cancel(true);
  }

  @Override
  public boolean isVanished(UUID uuid) {
    return vanishedPlayers.contains(uuid);
  }

  public List<MatchPlayer> getOnlineVanished() {
    return vanishedPlayers.stream()
        .filter(u -> matchManager.getPlayer(u) != null)
        .map(matchManager::getPlayer)
        .collect(Collectors.toList());
  }

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    final Match match = player.getMatch();

    // Call vanish event which will vanish/unvanish at appropriate levels
    match.callEvent(new PlayerVanishEvent(player, vanish, quiet));

    // Reset visibility to hide/show player
    player.resetVisibility();

    return isVanished(player.getId());
  }

  private void addVanished(MatchPlayer player) {
    if (!isVanished(player.getId())) {
      this.vanishedPlayers.add(player.getId());
      player.getBukkit().setMetadata(VANISH_KEY, VANISH_VALUE);
    }
  }

  private void removeVanished(MatchPlayer player) {
    this.vanishedPlayers.remove(player.getId());
    player.getBukkit().removeMetadata(VANISH_KEY, VANISH_VALUE.getOwningPlugin());
  }

  // Handle vanishing at high, so that medium priority (leave messages) is done before any change
  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerVanish(PlayerVanishEvent event) {
    if (!event.isVanished()) return;
    MatchPlayer player = event.getPlayer();
    addVanished(event.getPlayer());

    // Ensure player is an observer
    if (player.getParty() instanceof Competitor) {
      player.getMatch().setParty(player, player.getMatch().getDefaultParty());
    }
  }

  // Handle unvanishing at low, so by the time join message is shown player is already unvanished
  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerUnvanish(PlayerVanishEvent event) {
    if (event.isVanished()) return;
    removeVanished(event.getPlayer());
  }

  /* Events */
  private final Cache<UUID, String> loginSubdomains =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();
  private final List<UUID> tempVanish =
      Lists.newArrayList(); // List of online UUIDs who joined via "vanish" subdomain

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPreJoin(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    loginSubdomains.invalidate(player.getUniqueId());
    if (player.hasPermission(Permissions.VANISH)
        && !isVanished(player.getUniqueId())
        && isVanishSubdomain(event.getHostname())) {
      loginSubdomains.put(player.getUniqueId(), event.getHostname());
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onJoin(PlayerJoinEvent event) {
    MatchPlayer player = matchManager.getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.getParty() instanceof Competitor) return; // Do not vanish players on a team
    if (!player.getBukkit().hasPermission(Permissions.VANISH))
      return; // Do not vanish players with no perms

    // Automatic vanish if player logs in via a "vanish" subdomain
    String domain = loginSubdomains.getIfPresent(player.getId());
    if (domain != null) {
      loginSubdomains.invalidate(player.getId());
      tempVanish.add(player.getId());
      setVanished(player, true, true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onQuit(PlayerQuitEvent event) {
    MatchPlayer player = matchManager.getPlayer(event.getPlayer());
    // If player is vanished & joined via "vanish" subdomain. Remove vanish status on quit
    if (isVanished(player.getId()) && tempVanish.contains(player.getId())) {
      setVanished(player, false, true);
    }
  }

  @EventHandler
  public void onUnvanish(PlayerVanishEvent event) {
    // If player joined via "vanish" subdomain, but unvanishes while online
    // stop tracking them for auto-vanish removal
    if (!event.isVanished() && tempVanish.contains(event.getPlayer().getId())) {
      tempVanish.remove(event.getPlayer().getId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void checkMatchPlayer(PlayerJoinMatchEvent event) {
    MatchPlayer player = event.getPlayer();
    // Player is joining to a team so broadcast join
    if (event.getNewParty() instanceof Competitor && isVanished(player.getId())) {
      setVanished(player, false, true);
    }
  }

  private boolean isVanishSubdomain(String address) {
    return address.startsWith("vanish.");
  }

  private void sendHotbarVanish(MatchPlayer player, boolean flashColor) {
    Component warning = text(" \u26a0 ", flashColor ? NamedTextColor.YELLOW : NamedTextColor.GOLD);
    Component vanish = translatable("vanish.hotbar", NamedTextColor.RED, TextDecoration.BOLD);
    Component message = text().append(warning).append(vanish).append(warning).build();
    player.sendActionBar(message);
  }
}
