package tc.oc.pgm.listeners;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.named.NameStyle;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.chat.Sound;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.commands.SettingCommands;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.util.StringUtils;
import tc.oc.util.components.Components;
import tc.oc.world.OnlinePlayerMapAdapter;

public class ChatDispatcher implements Listener {

  private final MatchManager manager;
  private final OnlinePlayerMapAdapter<UUID> lastMessagedBy;

  private static final Sound DM_SOUND = new Sound("random.orb", 1f, 1.2f);

  public ChatDispatcher(MatchManager manager) {
    this.manager = manager;
    this.lastMessagedBy = new OnlinePlayerMapAdapter<>(PGM.get());
  }

  @Command(
      aliases = {"g", "all"},
      desc = "Send a message to everyone",
      usage = "[message]")
  public void sendGlobal(Match match, MatchPlayer sender, @Nullable @Text String message) {
    send(match, sender, message, "<{0}>: {1}", viewer -> true, SettingValue.CHAT_GLOBAL);
  }

  @Command(
      aliases = {"t"},
      desc = "Send a message to your team",
      usage = "[message]")
  public void sendTeam(Match match, MatchPlayer sender, @Nullable @Text String message) {
    final Party party = sender == null ? match.getDefaultParty() : sender.getParty();

    // No team chat when playing free-for-all or match end, default to global chat
    if (party instanceof Tribute || match.isFinished()) {
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
                    && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)),
        SettingValue.CHAT_TEAM);
  }

  @Command(
      aliases = {"a"},
      desc = "Send a message to operators",
      usage = "[message]",
      perms = Permissions.ADMINCHAT)
  public void sendAdmin(Match match, MatchPlayer sender, @Nullable @Text String message) {
    // If a player managed to send a default message without permissions, reset their chat channel
    if (sender != null && !sender.getBukkit().hasPermission(Permissions.ADMINCHAT)) {
      sender.getSettings().resetValue(SettingKey.CHAT);
      SettingKey.CHAT.update(sender);
      sender.sendWarning(
          "You do not have permissions for admin chat, your chat setting has now been reset",
          true); // TODO: translate
      return;
    }

    send(
        match,
        sender,
        message,
        "[" + ChatColor.GOLD + "A" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT),
        SettingValue.CHAT_ADMIN);
  }

  @Command(
      aliases = {"msg", "tell"},
      desc = "Send a direct message to a player",
      usage = "[player] [message]")
  public void sendDirect(Match match, MatchPlayer sender, Player receiver, @Text String message) {
    MatchPlayer matchReceiver = manager.getPlayer(receiver);
    if (matchReceiver != null) {
      SettingValue option = matchReceiver.getSettings().getValue(SettingKey.MESSAGE);

      if (option.equals(SettingValue.MESSAGE_OFF)
          && !sender.getBukkit().hasPermission(Permissions.STAFF)) {
        String name = receiver.getDisplayName(sender.getBukkit()) + ChatColor.RED;
        Component component =
            new PersonalizedTranslatable("command.message.blockedNoPermissions", name);
        sender.sendMessage(new PersonalizedText(component, ChatColor.RED));
        return;
      }
      playMessageSound(matchReceiver);
    }

    if (sender != null) {
      lastMessagedBy.put(receiver, sender.getId());
    }

    send(
        match,
        sender,
        message,
        "[" + ChatColor.GOLD + "DM" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().equals(receiver),
        null);

    send(
        match,
        manager.getPlayer(receiver), // Allow for cross-match messages
        message,
        "[" + ChatColor.GOLD + "DM" + ChatColor.WHITE + "] -> {0}: {1}",
        viewer -> viewer.getBukkit().equals(sender.getBukkit()),
        null);
  }

  @Command(
      aliases = {"r"},
      desc = "Reply to a direct message",
      usage = "[message]")
  public void sendReply(Match match, Audience audience, MatchPlayer sender, @Text String message) {
    final MatchPlayer receiver = manager.getPlayer(lastMessagedBy.get(sender.getBukkit()));
    if (receiver == null) {
      audience.sendWarning(
          new PersonalizedText("Did not find a message to reply to, use /msg")); // TODO: translate
      return;
    }

    sendDirect(match, sender, receiver.getBukkit(), message);
  }

  private static MatchPlayer getApproximatePlayer(Match match, String query, CommandSender sender) {
    return StringUtils.bestFuzzyMatch(
        query,
        match.getPlayers().stream()
            .collect(
                Collectors.toMap(
                    player -> player.getBukkit().getName(sender), Function.identity())),
        0.75);
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
        final MatchPlayer receiver =
            getApproximatePlayer(player.getMatch(), target, player.getBukkit());
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
    switch (sender == null
        ? SettingValue.CHAT_GLOBAL
        : sender.getSettings().getValue(SettingKey.CHAT)) {
      case CHAT_TEAM:
        sendTeam(match, sender, message);
        return;
      case CHAT_ADMIN:
        sendAdmin(match, sender, message);
        return;
      default:
        sendGlobal(match, sender, message);
    }
  }

  public void playMessageSound(MatchPlayer player) {
    if (player.getSettings().getValue(SettingKey.SOUNDS).equals(SettingValue.SOUNDS_ON)) {
      player.playSound(DM_SOUND);
    }
  }

  public void send(
      Match match,
      MatchPlayer sender,
      @Nullable String message,
      String format,
      Predicate<MatchPlayer> filter,
      @Nullable SettingValue type) {
    // When a message is empty, this indicates the player wants to change their default chat channel
    if (message == null) {
      try {
        SettingCommands.toggle(sender.getBukkit(), sender, SettingKey.CHAT, type.getName());
      } catch (ArgumentException e) {
        // No-op, this is when console tries to change chat settings
      }
      return;
    }

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
