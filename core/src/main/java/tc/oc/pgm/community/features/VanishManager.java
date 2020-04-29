package tc.oc.pgm.community.features;

import app.ashcon.intake.Command;
import app.ashcon.intake.CommandException;
import app.ashcon.intake.parametric.annotation.Switch;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
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
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.community.events.PlayerVanishEvent;
import tc.oc.pgm.listeners.ChatDispatcher;
import tc.oc.pgm.listeners.PGMListener;
import tc.oc.pgm.util.component.Component;
import tc.oc.pgm.util.component.ComponentRenderers;
import tc.oc.pgm.util.component.types.PersonalizedTranslatable;
import tc.oc.pgm.util.named.NameStyle;

public class VanishManager implements Listener {

  private static final String VANISH_KEY = "isVanished";
  private static final MetadataValue VANISH_VALUE = new FixedMetadataValue(PGM.get(), true);

  private List<UUID> vanishedPlayers;
  private final MatchManager matchManager;

  public VanishManager(MatchManager matchManager) {
    this.vanishedPlayers = Lists.newArrayList();
    this.matchManager = matchManager;
  }

  public boolean isVanished(UUID uuid) {
    return vanishedPlayers.contains(uuid);
  }

  private void addVanished(UUID uuid) {
    if (!isVanished(uuid)) {
      this.vanishedPlayers.add(uuid);
    }
  }

  private void removeVanished(UUID uuid) {
    this.vanishedPlayers.remove(uuid);
  }

  public List<MatchPlayer> getOnlineVanished() {
    return vanishedPlayers.stream()
        .filter(u -> matchManager.getPlayer(u) != null)
        .map(matchManager::getPlayer)
        .collect(Collectors.toList());
  }

  public void setVanished(MatchPlayer player, boolean vanish, boolean quiet)
      throws CommandException {
    if (isVanished(player.getId()) == vanish) {
      throw new CommandException(
          ComponentRenderers.toLegacyText(
              new PersonalizedTranslatable(
                  vanish ? "vanish.activate.already" : "vanish.deactivate.already"),
              player.getBukkit()));
    }

    // Keep track of the UUID and apply/remove META data, so we can detect vanish status from other
    // projects (i.e utils)
    if (vanish) {
      addVanished(player.getId());
      player.getBukkit().setMetadata(VANISH_KEY, VANISH_VALUE);
      player.getSettings().setValue(SettingKey.CHAT, SettingValue.CHAT_ADMIN);
    } else {
      removeVanished(player.getId());
      player.getBukkit().removeMetadata(VANISH_KEY, VANISH_VALUE.getOwningPlugin());
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
  }

  /* Commands */

  @Command(
      aliases = {"vanish", "disappear", "v"},
      desc = "Vanish from the server",
      perms = Permissions.STAFF)
  public void vanish(CommandSender sender, MatchPlayer player, @Switch('s') boolean silent)
      throws CommandException {
    setVanished(player, true, silent);
    sender.sendMessage(
        new PersonalizedTranslatable("vanish.activate")
            .getPersonalizedText()
            .color(ChatColor.GREEN));
  }

  @Command(
      aliases = {"unvanish", "appear", "uv"},
      desc = "Return to the server",
      perms = Permissions.STAFF)
  public void unVanish(CommandSender sender, MatchPlayer player, @Switch('s') boolean silent)
      throws CommandException {
    setVanished(player, false, silent);
    sender.sendMessage(
        new PersonalizedTranslatable("vanish.deactivate")
            .getPersonalizedText()
            .color(ChatColor.RED));
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

  public static Component formatVanishedName(Component vanishedPlayer) {
    return vanishedPlayer.color(ChatColor.DARK_GRAY).italic(true);
  }
}
