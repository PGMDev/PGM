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
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.listeners.PGMListener;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.types.PersonalizedText;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

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
    if (isVanished(player.getId()) == vanish) {
      return false;
    }

    // Keep track of the UUID and apply/remove META data, so we can detect vanish status from other
    // projects (i.e utils)
    if (vanish) {
      addVanished(player);
    } else {
      removeVanished(player);
    }

    final Match match = player.getMatch();

    // Ensure player is an observer
    if (!player.getParty().isObserving()) {
      match.setParty(player, match.getDefaultParty());
    }

    // Set vanish status in match player
    player.setVanished(vanish);

    // Reset visibility to hide/show player
    player.resetVisibility();

    // Broadcast join/quit message
    if (!quiet) {
      PGMListener.announceJoinOrLeave(
          player, vanish ? "broadcast.leaveMessage" : "broadcast.joinMessage");
    }

    match.callEvent(new PlayerVanishEvent(player, vanish));

    return true;
  }

  private void addVanished(MatchPlayer player) {
    if (!isVanished(player.getId())) {
      this.vanishedPlayers.add(player.getId());
      player.getBukkit().setMetadata(VANISH_KEY, VANISH_VALUE);
      player.getSettings().setValue(SettingKey.CHAT, SettingValue.CHAT_ADMIN);
    }
  }

  private void removeVanished(MatchPlayer player) {
    this.vanishedPlayers.remove(player.getId());
    player.getBukkit().removeMetadata(VANISH_KEY, VANISH_VALUE.getOwningPlugin());
  }

  /* Commands */
  @Command(
      aliases = {"vanish", "disappear", "v"},
      desc = "Vanish from the server",
      perms = Permissions.VANISH)
  public void vanish(MatchPlayer sender, @Switch('s') boolean silent) throws CommandException {
    if (setVanished(sender, true, silent)) {
      sender.sendMessage(TranslatableComponent.of("vanish.activate").color(TextColor.GREEN));
    } else {
      sender.sendWarning(TranslatableComponent.of("vanish.activate.already"));
    }
  }

  @Command(
      aliases = {"unvanish", "appear", "uv"},
      desc = "Return to the server",
      perms = Permissions.VANISH)
  public void unVanish(MatchPlayer sender, @Switch('s') boolean silent) throws CommandException {
    if (setVanished(sender, false, silent)) {
      sender.sendMessage(TranslatableComponent.of("vanish.deactivate").color(TextColor.RED));
    } else {
      sender.sendWarning(TranslatableComponent.of("vanish.deactivate.already"));
    }
  }

  /* Events */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    announceJoinOrLeaveForStaff("vanish.broadcast.join", event.getPlayer());

    if (isVanished(event.getPlayer().getUniqueId())) {
      matchManager.getPlayer(event.getPlayer()).setVanished(true);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    announceJoinOrLeaveForStaff("vanish.broadcast.leave", event.getPlayer());
  }

  @EventHandler
  public void checkMatchPlayer(MatchPlayerAddEvent event) {
    event.getPlayer().setVanished(isVanished(event.getPlayer().getId()));
  }

  private void announceJoinOrLeaveForStaff(String key, Player player) {
    Match match = matchManager.getMatch(player);
    MatchPlayer matchPlayer = match.getPlayer(player);
    if (isVanished(matchPlayer.getId())) {
      ChatDispatcher.broadcastAdminChatMessage(
          new PersonalizedTranslatable(key, matchPlayer.getStyledName(NameStyle.FANCY))
              .getPersonalizedText()
              .color(ChatColor.YELLOW),
          match);
    }
  }

  private void sendHotbarVanish(MatchPlayer player, boolean flashColor) {
    PersonalizedText warning =
        new PersonalizedText(" \u26a0 ", flashColor ? ChatColor.YELLOW : ChatColor.GOLD);
    Component vanish =
        new PersonalizedTranslatable("vanish.hotbar")
            .getPersonalizedText()
            .color(ChatColor.LIGHT_PURPLE)
            .bold(true);
    player.sendHotbarMessage(new PersonalizedText(warning, vanish, warning));
  }
}
