package tc.oc.pgm.community.features;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.listeners.PGMListener;

public class VanishManagerImpl implements VanishManager, Listener {

  private static final String VANISH_KEY = "isVanished";
  private static final MetadataValue VANISH_VALUE = new FixedMetadataValue(PGM.get(), true);

  private final List<UUID> vanishedPlayers;
  private final MatchManager matchManager;

  private final Future<?>
      hotbarTask; // Task is run every second to ensure vanished players retain hotbar message
  private boolean hotbarFlash;

  public VanishManagerImpl(MatchManager matchManager, ScheduledExecutorService tasks) {
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
  }

  @Override
  public void disable() {
    hotbarTask.cancel(true);
  }

  @Override
  public boolean isVanished(UUID uuid) {
    return vanishedPlayers.contains(uuid);
  }

  @Override
  public List<MatchPlayer> getOnlineVanished() {
    return vanishedPlayers.stream()
        .filter(u -> matchManager.getPlayer(u) != null)
        .map(matchManager::getPlayer)
        .collect(Collectors.toList());
  }

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    // Keep track of the UUID and apply/remove META data, so we can detect vanish status from other
    // projects (i.e utils)
    if (vanish) {
      addVanished(player);
    } else {
      removeVanished(player);
    }

    final Match match = player.getMatch();

    // Ensure player is an observer
    match.setParty(player, match.getDefaultParty());

    // Set vanish status in match player
    player.setVanished(vanish);

    // Reset visibility to hide/show player
    player.resetVisibility();

    // Broadcast join/quit message
    if (!quiet) {
      PGMListener.announceJoinOrLeave(player, !vanish, false);
    }

    match.callEvent(new PlayerVanishEvent(player, vanish));

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

  /* Commands */
  @Command(
      aliases = {"vanish", "v"},
      desc = "Toggle vanish status",
      perms = Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Switch('s') boolean silent) throws CommandException {
    if (setVanished(sender, !isVanished(sender.getId()), silent)) {
      sender.sendWarning(TranslatableComponent.of("vanish.activate").color(TextColor.GREEN));
    } else {
      sender.sendWarning(TranslatableComponent.of("vanish.deactivate").color(TextColor.RED));
    }
  }

  /* Events */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    if (isVanished(event.getPlayer().getUniqueId())) {
      matchManager.getPlayer(event.getPlayer()).setVanished(true);
    }
  }

  @EventHandler
  public void checkMatchPlayer(MatchPlayerAddEvent event) {
    event.getPlayer().setVanished(isVanished(event.getPlayer().getId()));
  }

  private void sendHotbarVanish(MatchPlayer player, boolean flashColor) {
    Component warning =
        TextComponent.of(" \u26a0 ", flashColor ? TextColor.YELLOW : TextColor.GOLD);
    Component vanish =
        TranslatableComponent.of("vanish.hotbar", TextColor.RED, TextDecoration.BOLD);
    Component message =
        TextComponent.builder().append(warning).append(vanish).append(warning).build();
    player.showHotbar(message);
  }
}
