package tc.oc.pgm.listeners;

import app.ashcon.intake.Command;
import app.ashcon.intake.argument.ArgumentException;
import app.ashcon.intake.parametric.annotation.Text;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.commands.SettingCommands;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public class ChatDispatcher implements Listener {

  private final MatchManager manager;
  private final VanishManager vanish;
  private final OnlinePlayerMapAdapter<UUID> lastMessagedBy;

  private final Set<UUID> muted;

  public static final TextComponent ADMIN_CHAT_PREFIX =
      TextComponent.builder()
          .append("[", TextColor.WHITE)
          .append("A", TextColor.GOLD)
          .append("] ", TextColor.WHITE)
          .build();

  private static final Sound DM_SOUND = new Sound("random.orb", 1f, 1.2f);
  private static final Sound AC_SOUND = new Sound("random.orb", 1f, 0.7f);

  private static final String GLOBAL_SYMBOL = "!";
  private static final String DM_SYMBOL = "@";
  private static final String ADMIN_CHAT_SYMBOL = "$";

  private static final String GLOBAL_FORMAT = "<%s>: %s";
  private static final String PREFIX_FORMAT = "%s: %s";
  private static final String AC_FORMAT =
      TextTranslations.translateLegacy(ADMIN_CHAT_PREFIX, null) + PREFIX_FORMAT;

  private static final Predicate<MatchPlayer> AC_FILTER =
      viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT);

  public ChatDispatcher(MatchManager manager, VanishManager vanish) {
    this.manager = manager;
    this.vanish = vanish;
    this.lastMessagedBy = new OnlinePlayerMapAdapter<>(PGM.get());
    this.muted = Sets.newHashSet();
  }

  public void addMuted(MatchPlayer player) {
    this.muted.add(player.getId());
  }

  public void removeMuted(MatchPlayer player) {
    this.muted.remove(player.getId());
  }

  public boolean isMuted(MatchPlayer player) {
    return player != null ? muted.contains(player.getId()) : false;
  }

  public Set<UUID> getMutedUUIDs() {
    return muted;
  }

  @Command(
      aliases = {"g", "all"},
      desc = "Send a message to everyone",
      usage = "[message]")
  public void sendGlobal(Match match, MatchPlayer sender, @Nullable @Text String message) {
    if (checkMute(sender)) {
      send(match, sender, message, GLOBAL_FORMAT, viewer -> true, SettingValue.CHAT_GLOBAL);
    }
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

    if (checkMute(sender)) {
      send(
          match,
          sender,
          message,
          party.getChatPrefix().toLegacyText() + PREFIX_FORMAT,
          viewer ->
              party.equals(viewer.getParty())
                  || (viewer.isObserving()
                      && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)),
          SettingValue.CHAT_TEAM);
    }
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
      sender.sendWarning(TranslatableComponent.of("misc.noPermission"));
      return;
    }

    send(
        match,
        sender,
        message != null ? BukkitUtils.colorize(message) : message,
        AC_FORMAT,
        AC_FILTER,
        SettingValue.CHAT_ADMIN);

    // Play sounds for admin chat
    if (message != null) {
      match.getPlayers().stream()
          .filter(AC_FILTER) // Initial filter
          .filter(viewer -> !viewer.equals(sender)) // Don't play sound for sender
          .forEach(pl -> playSound(pl, AC_SOUND));
    }
  }

  @Command(
      aliases = {"msg", "tell", "pm", "dm"},
      desc = "Send a direct message to a player",
      usage = "[player] [message]")
  public void sendDirect(Match match, MatchPlayer sender, Player receiver, @Text String message) {
    if (sender == null) return;

    if (vanish.isVanished(sender.getId())) {
      sender.sendWarning(TranslatableComponent.of("vanish.chat.deny"));
      return;
    }

    if (isMuted(sender) && !receiver.hasPermission(Permissions.STAFF)) {
      sendMutedMessage(sender);
      return; // Muted players may only message staff
    }

    MatchPlayer matchReceiver = manager.getPlayer(receiver);
    if (matchReceiver != null) {

      // Vanish Check - Don't allow messages to vanished
      if (vanish.isVanished(matchReceiver.getId())) {
        sender.sendWarning(TranslatableComponent.of("command.playerNotFound"));
        return;
      }

      SettingValue option = matchReceiver.getSettings().getValue(SettingKey.MESSAGE);

      if (option.equals(SettingValue.MESSAGE_OFF)
          && !sender.getBukkit().hasPermission(Permissions.STAFF)) {
        Component blocked =
            TranslatableComponent.of("command.message.blocked")
                .args(matchReceiver.getName(NameStyle.FANCY));
        sender.sendWarning(blocked);
        return;
      }

      if (isMuted(matchReceiver) && !sender.getBukkit().hasPermission(Permissions.STAFF)) {
        Component muted =
            TranslatableComponent.of("moderation.mute.target")
                .args(matchReceiver.getName(NameStyle.CONCISE));
        sender.sendWarning(muted);
        return; // Only staff can message muted players
      } else {
        playSound(matchReceiver, DM_SOUND);
      }
    }

    if (sender != null) {
      lastMessagedBy.put(receiver, sender.getId());
    }

    // Send message to receiver
    send(
        match,
        sender,
        message,
        formatPrivateMessage("misc.from", matchReceiver.getBukkit()),
        viewer -> viewer.getBukkit().equals(receiver),
        null);

    // Send message to the sender
    send(
        match,
        manager.getPlayer(receiver), // Allow for cross-match messages
        message,
        formatPrivateMessage("misc.to", sender.getBukkit()),
        viewer -> viewer.getBukkit().equals(sender.getBukkit()),
        null);
  }

  private String formatPrivateMessage(String key, CommandSender viewer) {
    Component action =
        TranslatableComponent.of(key, TextColor.GRAY).decoration(TextDecoration.ITALIC, true);
    return TextTranslations.translateLegacy(action, viewer) + " " + PREFIX_FORMAT;
  }

  @Command(
      aliases = {"reply", "r"},
      desc = "Reply to a direct message",
      usage = "[message]")
  public void sendReply(Match match, Audience audience, MatchPlayer sender, @Text String message) {
    if (sender == null) return;
    final MatchPlayer receiver = manager.getPlayer(lastMessagedBy.get(sender.getBukkit()));
    if (receiver == null) {
      audience.sendWarning(
          TranslatableComponent.of("command.message.noReply").args(TextComponent.of("/msg")));
      return;
    }

    sendDirect(match, sender, receiver.getBukkit(), message);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChat(AsyncPlayerChatEvent event) {
    if (CHAT_EVENT_CACHE.getIfPresent(event) == null) {
      event.setCancelled(true);
    } else {
      CHAT_EVENT_CACHE.invalidate(event);
      return;
    }

    final MatchPlayer player = manager.getPlayer(event.getPlayer());
    if (player != null) {
      final String message = event.getMessage();

      if (message.startsWith(GLOBAL_SYMBOL)) {
        sendGlobal(player.getMatch(), player, message.substring(1));
      } else if (message.startsWith(DM_SYMBOL)) {
        final String target = message.substring(1, message.indexOf(" "));
        final MatchPlayer receiver =
            getApproximatePlayer(player.getMatch(), target, player.getBukkit());
        if (receiver == null) {
          player.sendWarning(
              TranslatableComponent.of("chat.message.unknownTarget")
                  .args(TextComponent.of(target)));
        } else {
          sendDirect(
              player.getMatch(),
              player,
              receiver.getBukkit(),
              message.replace(target, "").substring(1));
        }
      } else if (message.startsWith(ADMIN_CHAT_SYMBOL)
          && player.getBukkit().hasPermission(Permissions.ADMINCHAT)) {
        sendAdmin(player.getMatch(), player, event.getMessage().substring(1));
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

  public void playSound(MatchPlayer player, Sound sound) {
    SettingValue value = player.getSettings().getValue(SettingKey.SOUNDS);
    if ((sound.equals(AC_SOUND) && value.equals(SettingValue.SOUNDS_ALL))
        || (sound.equals(DM_SOUND) && !value.equals(SettingValue.SOUNDS_NONE))) {
      player.playSound(sound);
    }
  }

  private static final Cache<AsyncPlayerChatEvent, Boolean> CHAT_EVENT_CACHE =
      CacheBuilder.newBuilder().weakKeys().expireAfterWrite(15, TimeUnit.SECONDS).build();

  public void send(
      Match match,
      MatchPlayer sender,
      @Nullable String text,
      String format,
      Predicate<MatchPlayer> filter,
      @Nullable SettingValue type) {
    // When a message is empty, this indicates the player wants to change their default chat channel
    if (text == null) {
      try {
        SettingCommands.toggle(
            sender == null ? null : sender.getBukkit(), sender, SettingKey.CHAT, type.getName());
      } catch (ArgumentException e) {
        // No-op, this is when console tries to change chat settings
      }
      return;
    }

    final String message = text.trim();

    if (sender != null) {
      PGM.get()
          .getAsyncExecutor()
          .execute(
              () -> {
                final Predicate<MatchPlayer> finalFilter = sender.isVanished() ? AC_FILTER : filter;
                final String finalFormat = sender.isVanished() ? AC_FORMAT : format;
                final AsyncPlayerChatEvent event =
                    new AsyncPlayerChatEvent(
                        false,
                        sender.getBukkit(),
                        message,
                        match.getPlayers().stream()
                            .filter(finalFilter)
                            .map(MatchPlayer::getBukkit)
                            .collect(Collectors.toSet()));
                event.setFormat(finalFormat);
                CHAT_EVENT_CACHE.put(event, true);
                match.callEvent(event);

                if (event.isCancelled()) {
                  return;
                }

                final String finalMessage =
                    String.format(event.getFormat(), sender.getBukkit().getDisplayName(), message);
                event.getRecipients().forEach(player -> player.sendMessage(finalMessage));
              });
      return;
    }
    match.getPlayers().stream()
        .filter(filter)
        .forEach(
            player ->
                player.sendMessage(
                    String.format(
                        format,
                        TextTranslations.translate(
                            UsernameFormatUtils.CONSOLE_NAME, player.getBukkit().getLocale()),
                        message)));
  }

  private MatchPlayer getApproximatePlayer(Match match, String query, CommandSender sender) {
    return StringUtils.bestFuzzyMatch(
        query,
        match.getPlayers().stream()
            .collect(
                Collectors.toMap(
                    player -> player.getBukkit().getName(sender), Function.identity())),
        0.75);
  }

  private void sendMutedMessage(MatchPlayer player) {
    Component warning = TranslatableComponent.of("moderation.mute.message");
    player.sendWarning(warning);
  }

  private boolean checkMute(MatchPlayer player) {
    if (isMuted(player)) {
      sendMutedMessage(player);
      return false;
    }

    return true;
  }

  public static void broadcastAdminChatMessage(Component message, Match match) {
    broadcastAdminChatMessage(message, match, Optional.empty());
  }

  public static void broadcastAdminChatMessage(
      Component message, Match match, Optional<Sound> sound) {
    TextComponent formatted = ADMIN_CHAT_PREFIX.append(message);
    match.getPlayers().stream()
        .filter(AC_FILTER)
        .forEach(
            mp -> {
              // If provided a sound, play if setting allows
              sound.ifPresent(
                  s -> {
                    if (canPlaySound(mp)) {
                      mp.playSound(s);
                    }
                  });
              mp.sendMessage(formatted);
            });
    Audience.get(Bukkit.getConsoleSender()).sendMessage(formatted);
  }

  private static boolean canPlaySound(MatchPlayer viewer) {
    return viewer.getSettings().getValue(SettingKey.SOUNDS).equals(SettingValue.SOUNDS_ALL);
  }
}
