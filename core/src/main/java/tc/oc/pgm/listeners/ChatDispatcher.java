package tc.oc.pgm.listeners;

import static net.kyori.adventure.identity.Identity.identity;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.command.SettingCommand;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.bukkit.Sounds;
import tc.oc.pgm.util.channels.Channel;
import tc.oc.pgm.util.event.ChannelMessageEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextTranslations;

public class ChatDispatcher implements Listener {

  private static ChatDispatcher INSTANCE = new ChatDispatcher();

  public static ChatDispatcher get() {
    return INSTANCE; // FIXME: no one should need to statically access ChatDispatcher, but community
    // does this a lot
  }

  private final MatchManager manager;
  private final Map<UUID, MessageSenderIdentity> lastMessagedBy;

  public static final TextComponent ADMIN_CHAT_PREFIX = text()
      .append(text("[", NamedTextColor.WHITE))
      .append(text("A", NamedTextColor.GOLD))
      .append(text("] ", NamedTextColor.WHITE))
      .build();

  private static final String GLOBAL_SYMBOL = "!";
  private static final String DM_SYMBOL = "@";
  private static final String ADMIN_CHAT_SYMBOL = "$";

  private static final String GLOBAL_FORMAT = "<%s>: %s";
  private static final String PREFIX_FORMAT = "%s: %s";
  private static final String AC_FORMAT =
      TextTranslations.translateLegacy(ADMIN_CHAT_PREFIX) + PREFIX_FORMAT;

  private static final Predicate<MatchPlayer> AC_FILTER =
      viewer -> viewer.getBukkit().hasPermission(Permissions.ADMINCHAT);

  public ChatDispatcher() {
    this.manager = PGM.get().getMatchManager();
    this.lastMessagedBy = new HashMap<>();
    PGM.get().getServer().getPluginManager().registerEvents(this, PGM.get());
  }

  public boolean isMuted(MatchPlayer player) {
    return player != null && Integration.isMuted(player.getBukkit());
  }

  @Command("g|all [message]")
  @CommandDescription("Send a message to everyone")
  public void sendGlobal(
      Match match,
      @NotNull MatchPlayer sender,
      @Argument(value = "message", suggestions = "players") @Greedy String message) {
    if (Integration.isVanished(sender.getBukkit())) {
      sendAdmin(match, sender, message);
      return;
    }
    throwMuted(sender);

    send(
        match,
        sender,
        message,
        GLOBAL_FORMAT,
        getChatFormat(null, sender, message),
        match.getPlayers(),
        viewer -> true,
        SettingValue.CHAT_GLOBAL,
        Channel.GLOBAL);
  }

  @Command("t [message]")
  @CommandDescription("Send a message to your team")
  public void sendTeam(
      Match match,
      @NotNull MatchPlayer sender,
      @Argument(value = "message", suggestions = "players") @Greedy String message) {
    if (Integration.isVanished(sender.getBukkit())) {
      sendAdmin(match, sender, message);
      return;
    }

    final Party party = sender.getParty();

    // No team chat when playing free-for-all or match end, default to global chat
    if (party instanceof Tribute || match.isFinished()) {
      sendGlobal(match, sender, message);
      return;
    }

    throwMuted(sender);
    send(
        match,
        sender,
        message,
        TextTranslations.translateLegacy(party.getChatPrefix()) + PREFIX_FORMAT,
        getChatFormat(party.getChatPrefix(), sender, message),
        match.getPlayers(),
        viewer -> party.equals(viewer.getParty())
            || (viewer.isObserving() && viewer.getBukkit().hasPermission(Permissions.ADMINCHAT)),
        SettingValue.CHAT_TEAM,
        Channel.TEAM);
  }

  @Command("a [message]")
  @CommandDescription("Send a message to operators")
  @Permission(Permissions.ADMINCHAT)
  public void sendAdmin(
      Match match,
      @NotNull MatchPlayer sender,
      @Argument(value = "message", suggestions = "players") @Greedy String message) {
    // If a player managed to send a default message without permissions, reset their chat channel
    if (!sender.getBukkit().hasPermission(Permissions.ADMINCHAT)) {
      sender.getSettings().resetValue(SettingKey.CHAT);
      SettingKey.CHAT.update(sender);
      sender.sendWarning(translatable("misc.noPermission"));
      return;
    }

    send(
        match,
        sender,
        message != null ? BukkitUtils.colorize(message) : null,
        AC_FORMAT,
        getChatFormat(ADMIN_CHAT_PREFIX, sender, message),
        match.getPlayers(),
        AC_FILTER,
        SettingValue.CHAT_ADMIN,
        Channel.ADMIN);

    // Play sounds for admin chat
    if (message != null) {
      match.getPlayers().stream()
          .filter(AC_FILTER) // Initial filter
          .filter(viewer -> !viewer.equals(sender)) // Don't play sound for sender
          .forEach(pl -> playSound(pl, Sounds.ADMIN_CHAT));
    }
  }

  @Command("msg|tell|pm|dm <player> <message>")
  @CommandDescription("Send a direct message to a player")
  public void sendDirect(
      Match match,
      @NotNull MatchPlayer sender,
      @Argument("player") MatchPlayer receiver,
      @Argument(value = "message", suggestions = "players") @Greedy String message) {
    if (Integration.isVanished(sender.getBukkit())) throw exception("vanish.chat.deny");
    if (receiver.equals(sender)) throw exception("command.message.self");

    if (!receiver.getBukkit().hasPermission(Permissions.STAFF)) throwMuted(sender);

    SettingValue option = receiver.getSettings().getValue(SettingKey.MESSAGE);

    if (!sender.getBukkit().hasPermission(Permissions.STAFF)) {
      if (option.equals(SettingValue.MESSAGE_OFF))
        throw exception("command.message.blocked", receiver.getName());

      if (option.equals(SettingValue.MESSAGE_FRIEND)
          && !Integration.isFriend(receiver.getBukkit(), sender.getBukkit()))
        throw exception("command.message.friendsOnly", receiver.getName());

      if (isMuted(receiver)) throw exception("moderation.mute.target", receiver.getName());
    }

    trackMessage(receiver.getBukkit(), sender.getBukkit());

    // Send message to receiver
    send(
        match,
        sender,
        message,
        formatPrivateMessage("misc.from", receiver.getBukkit()),
        getChatFormat(
            text()
                .append(translatable("misc.from", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(space())
                .build(),
            sender,
            message),
        Collections.singleton(receiver),
        r -> true,
        null,
        Channel.PRIVATE_RECEIVER);

    // Send message to the sender
    send(
        match,
        receiver,
        message,
        formatPrivateMessage("misc.to", sender.getBukkit()),
        getChatFormat(
            text()
                .append(translatable("misc.to", NamedTextColor.GRAY, TextDecoration.ITALIC))
                .append(space())
                .build(),
            receiver,
            message),
        Collections.singleton(sender),
        s -> true,
        null,
        Channel.PRIVATE_SENDER);
    playSound(receiver, Sounds.DIRECT_MESSAGE);
  }

  private String formatPrivateMessage(String key, CommandSender viewer) {
    Component action =
        translatable(key, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true);
    return TextTranslations.translateLegacy(action, viewer) + " " + PREFIX_FORMAT;
  }

  @Command("reply|r <message>")
  @CommandDescription("Reply to a direct message")
  public void sendReply(
      Match match,
      @NotNull MatchPlayer sender,
      @Argument(value = "message", suggestions = "players") @Greedy String message) {
    MatchPlayer receiver = manager.getPlayer(getLastMessagedId(sender.getBukkit()));
    if (receiver == null) throw exception("command.message.noReply", text("/msg"));

    sendDirect(match, sender, receiver, message);
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
    if (player == null) return;

    final String message = event.getMessage();

    try {
      if (message.startsWith(GLOBAL_SYMBOL)) {
        sendGlobal(player.getMatch(), player, message.substring(1));
      } else if (message.startsWith(DM_SYMBOL) && message.contains(" ")) {
        final String target = message.substring(1, message.indexOf(" "));
        final MatchPlayer receiver = Players.getMatchPlayer(event.getPlayer(), target);
        if (receiver == null) {
          player.sendWarning(translatable("command.playerNotFound"));
        } else {
          sendDirect(player.getMatch(), player, receiver, message.substring(2 + target.length()));
        }
      } else if (message.startsWith(ADMIN_CHAT_SYMBOL)
          && player.getBukkit().hasPermission(Permissions.ADMINCHAT)) {
        sendAdmin(player.getMatch(), player, event.getMessage().substring(1));
      } else {
        sendDefault(player.getMatch(), player, event.getMessage());
      }
    } catch (TextException e) {
      // Allow sub-handlers to throw command exceptions just fine
      player.sendWarning(e);
    }
  }

  public void sendDefault(Match match, @NotNull MatchPlayer sender, String message) {
    switch (sender.getSettings().getValue(SettingKey.CHAT)) {
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

  private static final Cache<AsyncPlayerChatEvent, Boolean> CHAT_EVENT_CACHE =
      CacheBuilder.newBuilder()
          .weakKeys()
          .expireAfterWrite(15, TimeUnit.SECONDS)
          .build();

  public void send(
      final @NotNull Match match,
      final @NotNull MatchPlayer sender,
      final @Nullable String text,
      final @NotNull String format,
      final @NotNull Component componentMsg,
      final @NotNull Collection<MatchPlayer> matchPlayers,
      final @NotNull Predicate<MatchPlayer> filter,
      final @Nullable SettingValue type,
      final @NotNull Channel channel) {
    final String message = text == null ? null : text.trim();
    // When a message is empty, this indicates the player wants to change their default chat channel
    if (message == null || message.isEmpty()) {
      if (type != null) SettingCommand.getInstance().toggle(sender, SettingKey.CHAT, type);
      return;
    }

    final Set<Player> players = matchPlayers.stream()
        .filter(filter)
        .map(MatchPlayer::getBukkit)
        .collect(Collectors.toSet());

    Runnable completion =
        () -> syncSendChat(match, sender, message, format, componentMsg, players, channel);
    if (Bukkit.isPrimaryThread()) completion.run();
    else PGM.get().getExecutor().execute(completion);
  }

  private void syncSendChat(
      final @NotNull Match match,
      final @NotNull MatchPlayer sender,
      final @NotNull String message,
      final @NotNull String format,
      final @NotNull Component componentMsg,
      final @NotNull Set<Player> players,
      final @NotNull Channel channel) {
    final AsyncPlayerChatEvent event =
        new AsyncPlayerChatEvent(false, sender.getBukkit(), message, players);
    event.setFormat(format);
    CHAT_EVENT_CACHE.put(event, true);
    match.callEvent(event);

    if (event.isCancelled()) return;
    match.callEvent(new ChannelMessageEvent(channel, sender.getBukkit(), message));

    Identity senderId = identity(sender.getId());

    event.getRecipients().stream()
        .map(Audience::get)
        .forEach(player -> player.sendMessage(senderId, componentMsg));
  }

  private void throwMuted(MatchPlayer player) {
    if (isMuted(player)) {
      Optional<String> muteReason =
          Optional.ofNullable(Integration.getMuteReason(player.getBukkit()));

      Component reason = muteReason.isPresent()
          ? text(muteReason.get())
          : translatable("moderation.mute.noReason");

      throw exception("moderation.mute.message", reason.color(NamedTextColor.AQUA));
    }
  }

  public static void broadcastAdminChatMessage(Component message, Match match) {
    broadcastAdminChatMessage(message, match, Optional.empty());
  }

  public static void broadcastAdminChatMessage(
      Component message, Match match, Optional<Sound> sound) {
    TextComponent formatted = ADMIN_CHAT_PREFIX.append(message);
    match.getPlayers().stream().filter(AC_FILTER).forEach(mp -> {
      // If provided a sound, play if setting allows
      sound.ifPresent(s -> playSound(mp, s));
      mp.sendMessage(formatted);
    });
    Audience.console().sendMessage(formatted);
  }

  public static void playSound(MatchPlayer player, Sound sound) {
    SettingValue value = player.getSettings().getValue(SettingKey.SOUNDS);
    if (value.equals(SettingValue.SOUNDS_ALL)
        || value.equals(SettingValue.SOUNDS_CHAT)
        || (sound.equals(Sounds.DIRECT_MESSAGE) && value.equals(SettingValue.SOUNDS_DM))) {
      player.playSound(sound);
    }
  }

  private Component getChatFormat(@Nullable Component prefix, MatchPlayer player, String message) {
    Component msg = text(message != null ? message.trim() : "");
    if (prefix == null)
      return text()
          .append(text("<", NamedTextColor.WHITE))
          .append(player.getName(NameStyle.VERBOSE))
          .append(text(">: ", NamedTextColor.WHITE))
          .append(msg)
          .build();
    return text()
        .append(prefix)
        .append(player.getName(NameStyle.VERBOSE))
        .append(text(": ", NamedTextColor.WHITE))
        .append(msg)
        .build();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.lastMessagedBy.remove(event.getPlayer().getUniqueId());
  }

  private void trackMessage(Player receiver, Player sender) {
    this.lastMessagedBy.put(receiver.getUniqueId(), new MessageSenderIdentity(receiver, sender));
  }

  private UUID getLastMessagedId(Player sender) {
    MessageSenderIdentity targetIdent = lastMessagedBy.get(sender.getUniqueId());
    if (targetIdent == null) return null;
    MatchPlayer target = manager.getPlayer(targetIdent.getPlayerId());

    // Prevent replying to offline players
    if (target == null) return null;

    // Compare last known and current name
    String lastKnownName = targetIdent.getName();
    String currentName = Players.getVisibleName(sender, target.getBukkit());

    // Ensure the target is visible to the viewing sender
    boolean visible = Players.isVisible(sender, target.getBukkit());

    if (currentName.equalsIgnoreCase(lastKnownName) && visible) {
      return target.getId();
    }

    return null;
  }

  private static class MessageSenderIdentity {
    private final UUID playerId;
    private final String name;

    public MessageSenderIdentity(Player viewer, Player player) {
      this.playerId = player.getUniqueId();
      this.name = Players.getVisibleName(viewer, player);
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public String getName() {
      return name;
    }
  }
}
