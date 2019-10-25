package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Text;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tc.oc.chat.BukkitAudiences;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedPlayer;
import tc.oc.component.types.PersonalizedText;
import tc.oc.named.NameStyle;
import tc.oc.pgm.PGM;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.server.Permissions;
import tc.oc.util.components.Components;
import tc.oc.world.OnlinePlayerMapAdapter;

public class ChatCommands {

  private final OnlinePlayerMapAdapter<UUID> lastDirectMessage;

  public ChatCommands() {
    this.lastDirectMessage = new OnlinePlayerMapAdapter<>(PGM.get());
  }

  @Command(
      aliases = {"g"},
      desc = "Send a message to everyone",
      usage = "[message]")
  public void global(MatchPlayer sender, @Text String message) {
    send(sender, message, "<{0}> {1}", viewer -> true);
  }

  @Command(
      aliases = {"t"},
      desc = "Send a message to your team",
      usage = "[message]")
  public void team(MatchPlayer sender, @Text String message) {
    // No team chat when playing free-for-all, default to global chat
    if (sender.getParty() instanceof Tribute) {
      global(sender, message);
      return;
    }

    send(
        sender,
        message,
        sender.getParty().getChatPrefix().toLegacyText() + "{0}: {1}",
        viewer ->
            sender.getParty().equals(viewer.getParty())
                || (viewer.isObserving()
                    && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)));
  }

  @Command(
      aliases = {"a"},
      desc = "Send a message to operators",
      usage = "[message]",
      perms = Permissions.ADMINCHAT)
  public void admin(MatchPlayer sender, @Text String message) {
    send(
        sender,
        message,
        "[" + ChatColor.GOLD + "A" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT));
  }

  @Command(
      aliases = {"msg"},
      desc = "Send a direct message to a player",
      usage = "[player] [message]")
  public void direct(MatchPlayer sender, Player receiver, @Text String message) {
    lastDirectMessage.put(sender.getBukkit(), receiver.getUniqueId());
    lastDirectMessage.putIfAbsent(receiver, sender.getPlayerId());
    send(
        sender,
        message,
        "[" + ChatColor.GOLD + "DM" + ChatColor.WHITE + "] {0}: {1}",
        viewer -> viewer.getBukkit().equals(receiver));
  }

  @Command(
      aliases = {"r"},
      desc = "Reply to a direct message",
      usage = "[message]")
  public void reply(MatchPlayer sender, @Text String message) {
    final MatchPlayer receiver =
        sender.getMatch().getPlayer(lastDirectMessage.get(sender.getBukkit()));
    if (receiver == null) {
      sender.sendWarning("Did not find a message to reply to, use /msg"); // TODO: translate
      return;
    }

    direct(sender, receiver.getBukkit(), message);
  }

  // FIXME: commands will crash when used as console, need to change provider logic
  public void send(
      MatchPlayer sender, String message, String format, Predicate<MatchPlayer> viewerFilter) {
    if (message == null || message.trim().isEmpty()) {
      sender.sendWarning("Cannot send an empty chat message", true); // TODO: translate
      return;
    }

    final AsyncPlayerChatEvent event =
        new AsyncPlayerChatEvent(
            true,
            sender.getBukkit(),
            message.trim(),
            sender.getMatch().getPlayers().stream()
                .filter(viewerFilter)
                .map(MatchPlayer::getBukkit)
                .collect(Collectors.toSet()));
    if (format != null) {
      event.setFormat(format);
    }

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            PGM.get(),
            () -> {
              Bukkit.getPluginManager().callEvent(event);

              if (!event.isCancelled()) {
                final Component component =
                    new PersonalizedText(
                        Components.format(
                            event.getFormat(),
                            new PersonalizedPlayer(event.getPlayer(), NameStyle.FANCY),
                            new PersonalizedText(message)));

                event
                    .getRecipients()
                    .forEach(player -> BukkitAudiences.getAudience(player).sendMessage(component));
              }
            });
  }
}
