package tc.oc.pgm.listeners;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.util.components.Components;
import tc.oc.world.OnlinePlayerMapAdapter;

public class ChatDispatcher implements Listener {

  private final MatchManager manager;
  private final OnlinePlayerMapAdapter<UUID> lastDirectMessage;

  public ChatDispatcher(MatchManager manager) {
    this.manager = manager;
    this.lastDirectMessage = new OnlinePlayerMapAdapter<>(PGM.get());
  }

  @Command(
      aliases = {"g"},
      desc = "Send a message to everyone",
      usage = "[message]")
  public void sendGlobal(Match match, MatchPlayer sender, @Nullable @Text String message) {
    send(match, sender, message, "<{0}>: {1}", viewer -> true);
  }

  @Command(
      aliases = {"t"},
      desc = "Send a message to your team",
      usage = "[message]")
  public void sendTeam(Match match, MatchPlayer sender, @Nullable @Text String message) {
    final Party party = sender == null ? match.getDefaultParty() : sender.getParty();

    // No team chat when playing free-for-all, default to global chat
    if (party instanceof Tribute) {
      sendGlobal(match, sender, message);
      return;
    }

    send(
        match,
        sender,
        message,
        party.getChatPrefix().toLegacyText() + "{0}: {1}",
        viewer ->
            party.equals(viewer.getParty())
                || (viewer.isObserving()
                    && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)));
  }

  @Command(
      aliases = {"a"},
      desc = "Send a message to operators",
      usage = "[message]",
      perms = Permissions.ADMINCHAT)
  public void sendAdmin(Match match, MatchPlayer sender, @Nullable @Text String message) {
    send(
        match,
        sender,
        message,
        "[" + ChatColor.GOLD + "A" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT));
  }

  @Command(
      aliases = {"msg", "tell"},
      desc = "Send a direct message to a player",
      usage = "[player] [message]")
  public void sendDirect(Match match, MatchPlayer sender, Player receiver, @Text String message) {
    if (sender != null) {
      lastDirectMessage.put(sender.getBukkit(), receiver.getUniqueId());
      lastDirectMessage.putIfAbsent(receiver, sender.getId());
    }

    send(
        match,
        sender,
        message,
        "[" + ChatColor.GOLD + "DM" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().equals(receiver));

    send(
        match,
        manager.getPlayer(receiver), // Allow for cross-match messages
        message,
        "[" + ChatColor.GOLD + "DM" + ChatColor.WHITE + "] -> {0}: {1}",
        viewer -> viewer.getBukkit().equals(sender.getBukkit()));
  }

  @Command(
      aliases = {"r"},
      desc = "Reply to a direct message",
      usage = "[message]")
  public void sendReply(Match match, Audience audience, MatchPlayer sender, @Text String message) {
    final MatchPlayer receiver = manager.getPlayer(lastDirectMessage.get(sender.getBukkit()));
    if (receiver == null) {
      audience.sendWarning(
          new PersonalizedText("Did not find a message to reply to, use /msg")); // TODO: translate
      return;
    }

    sendDirect(match, sender, receiver.getBukkit(), message);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChat(AsyncPlayerChatEvent event) {
    event.setCancelled(true);

    final MatchPlayer player = manager.getPlayer(event.getPlayer());
    if (player != null) {
      final String message = event.getMessage();

      if (message.startsWith("!")) {
        sendGlobal(player.getMatch(), player, message.substring(1));
      } else if (message.startsWith("@")) {
        final String target = message.substring(1, message.indexOf(" "));
        final MatchPlayer receiver = manager.findPlayer(target, player.getBukkit());

        if (receiver == null) {
          player.sendWarning("Could not find player '" + target + "' to send a message", true);
        } else {
          sendDirect(
              player.getMatch(),
              player,
              receiver.getBukkit(),
              message.replace(target, "").substring(1));
        }
      } else {
        sendDefault(player.getMatch(), player, event.getMessage());
      }
    }
  }

  public void sendDefault(Match match, MatchPlayer sender, String message) {
    switch (sender == null ? SettingValue.GLOBAL : sender.getSetting().getValue(SettingKey.CHAT)) {
      case TEAM:
        sendTeam(match, sender, message);
        return;
      case ADMIN:
        sendAdmin(match, sender, message);
        return;
      default:
        sendGlobal(match, sender, message);
    }
  }

  public void send(
      Match match,
      MatchPlayer sender,
      String message,
      String format,
      Predicate<MatchPlayer> filter) {
    final Component component =
        new PersonalizedText(
            Components.format(
                format,
                sender == null
                    ? new PersonalizedText("Console", ChatColor.AQUA, ChatColor.ITALIC)
                    : sender.getStyledName(NameStyle.FANCY),
                new PersonalizedText(message.trim())));
    match.getPlayers().stream().filter(filter).forEach(player -> player.sendMessage(component));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(component);
  }
}
