package tc.oc.pgm.channels;

import static net.kyori.adventure.identity.Identity.identity;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.noPermission;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.text.TextException;

public class ChannelManager implements Listener {

  private final GlobalChannel globalChannel;
  private final AdminChannel adminChannel;
  private final TeamChannel teamChannel;
  private final Set<Channel<?>> channels;
  private final Map<Character, Channel<?>> shortcuts;
  private final OnlinePlayerMapAdapter<Channel<?>> selectedChannel;

  private static final Cache<AsyncPlayerChatEvent, Boolean> CHAT_EVENT_CACHE =
      CacheBuilder.newBuilder().weakKeys().expireAfterWrite(15, TimeUnit.SECONDS).build();

  public ChannelManager() {
    this.channels = new HashSet<Channel<?>>();
    this.channels.add(globalChannel = new GlobalChannel());
    this.channels.add(adminChannel = new AdminChannel());
    this.channels.add(teamChannel = new TeamChannel());
    this.channels.add(new MessageChannel(this));
    this.channels.addAll(Integration.getRegisteredChannels());
    Integration.finishRegisteringChannels();

    this.shortcuts = new HashMap<Character, Channel<?>>();
    for (Channel<?> channel : channels) {
      if (channel.getShortcut() == null) continue;

      this.shortcuts.putIfAbsent(channel.getShortcut(), channel);
    }

    this.selectedChannel = new OnlinePlayerMapAdapter<Channel<?>>(PGM.get());
  }

  public void processChat(MatchPlayer sender, String message) {
    if (message.isEmpty()) return;

    Channel<?> channel = getSelectedChannel(sender);
    Map<String, Object> arguments = new HashMap<String, Object>();
    arguments.put("message", message);

    Channel<?> shortcut = shortcuts.get(message.charAt(0));
    if (shortcut != null && shortcut.canSendMessage(sender)) {
      arguments = shortcut.processChatShortcut(sender, message);
      channel = shortcut;
    }

    if (arguments.containsKey("message")) process(channel, sender, arguments);
  }

  public void process(Channel<?> channel, MatchPlayer sender, Map<String, ?> arguments) {
    process0(calculateChannelRedirect(channel, sender), sender, arguments);
  }

  private <T> void process0(Channel<T> channel, MatchPlayer sender, Map<String, ?> arguments) {
    if (!channel.canSendMessage(sender)) throw noPermission();
    throwMuted(sender);

    T target = channel.getTarget(sender, arguments);
    Collection<MatchPlayer> viewers = channel.getViewers(target);

    final AsyncPlayerChatEvent asyncEvent =
        new AsyncPlayerChatEvent(
            false,
            sender.getBukkit(),
            (String) arguments.get("message"),
            viewers.stream().map(MatchPlayer::getBukkit).collect(Collectors.toSet()));
    CHAT_EVENT_CACHE.put(asyncEvent, true);
    sender.getMatch().callEvent(asyncEvent);
    if (asyncEvent.isCancelled()) return;

    // The actual message is sent in sendMessage(ChannelMessageEvent)
    final ChannelMessageEvent<T> channelEvent =
        new ChannelMessageEvent<T>(channel, sender, target, viewers, asyncEvent.getMessage());
    channel.sendMessage(channelEvent);
    sender.getMatch().callEvent(channelEvent);
  }

  private Channel<?> calculateChannelRedirect(Channel<?> channel, MatchPlayer sender) {
    if (Integration.isVanished(sender.getBukkit()) && !(channel instanceof AdminChannel)) {
      if (!channel.supportsRedirect()) throw exception("vanish.chat.deny");
      return adminChannel;
    }

    if (sender.getMatch().isFinished() || sender.getParty() instanceof Tribute) {
      if (channel instanceof TeamChannel) return globalChannel;
    }

    return channel;
  }

  private void throwMuted(MatchPlayer player) {
    if (!Integration.isMuted(player.getBukkit())) return;
    Optional<String> muteReason =
        Optional.ofNullable(Integration.getMuteReason(player.getBukkit()));
    Component reason =
        muteReason.isPresent() ? text(muteReason.get()) : translatable("moderation.mute.noReason");

    throw exception("moderation.mute.message", reason.color(NamedTextColor.AQUA));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public <T> void sendMessage(ChannelMessageEvent<T> event) {
    if (event.isCancelled()) {
      if (event.getSender() != null && event.getCancellationReason() != null)
        event.getSender().sendWarning(event.getCancellationReason());
      return;
    }

    Component finalMessage =
        event
            .getChannel()
            .formatMessage(event.getTarget(), event.getSender(), text(event.getMessage()));
    Identity senderId = identity(event.getSender().getId());
    event.getViewers().forEach(player -> player.sendMessage(senderId, finalMessage));
  }

  @EventHandler
  public void onPlayerTabComplete(PlayerChatTabCompleteEvent event) {
    if (event.getChatMessage().trim().equals(event.getLastToken())) {
      char first = event.getLastToken().charAt(0);
      if (shortcuts.get(first) != null) {
        List<String> suggestions =
            Players.getPlayerNames(event.getPlayer(), event.getLastToken().substring(1));
        suggestions.replaceAll(s -> first + s);

        event.getTabCompletions().addAll(suggestions);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    selectedChannel.put(
        player.getBukkit(), findChannelBySetting(player.getSettings().getValue(SettingKey.CHAT)));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChat(AsyncPlayerChatEvent event) {
    if (CHAT_EVENT_CACHE.getIfPresent(event) == null) {
      event.setCancelled(true);
    } else {
      CHAT_EVENT_CACHE.invalidate(event);
      return;
    }

    final MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;

    final String message = event.getMessage().trim();
    try {
      processChat(player, message);
    } catch (TextException e) {
      // Allow sub-handlers to throw command exceptions just fine
      player.sendWarning(e);
    }
  }

  private Channel<?> findChannelBySetting(SettingValue setting) {
    for (Channel<?> channel : channels) if (setting == channel.getSetting()) return channel;

    return globalChannel;
  }

  public void setChannel(MatchPlayer player, SettingValue value) {
    selectedChannel.put(player.getBukkit(), findChannelBySetting(value));
  }

  public void setChannel(MatchPlayer player, Channel<?> channel) {
    Channel<?> previous = selectedChannel.get(player.getBukkit());
    selectedChannel.put(player.getBukkit(), channel);

    if (channel.getSetting() != null) {
      Settings setting = player.getSettings();
      final SettingValue old = setting.getValue(SettingKey.CHAT);

      if (old != channel.getSetting()) {
        setting.setValue(SettingKey.CHAT, channel.getSetting());
      }
    }

    if (previous != channel) {
      player.sendMessage(
          translatable(
              "setting.set",
              text("chat"),
              text(previous.getDisplayName(), NamedTextColor.GRAY),
              text(channel.getDisplayName(), NamedTextColor.GREEN)));
    } else {
      player.sendMessage(
          translatable(
              "setting.get", text("chat"), text(previous.getDisplayName(), NamedTextColor.GREEN)));
    }
  }

  public Channel<?> getSelectedChannel(MatchPlayer player) {
    return selectedChannel.getOrDefault(player.getBukkit(), globalChannel);
  }

  public Set<Channel<?>> getChannels() {
    return channels;
  }

  public AdminChannel getAdminChannel() {
    return adminChannel;
  }

  public static void broadcastMessage(Component message) {
    PGM.get().getChannelManager().globalChannel.broadcastMessage(message, null);
  }

  public static void broadcastMessage(Component message, Predicate<MatchPlayer> filter) {
    PGM.get().getChannelManager().globalChannel.broadcastMessage(message, null, filter);
  }

  public static void broadcastAdminMessage(Component message) {
    PGM.get().getChannelManager().adminChannel.broadcastMessage(message, null);
  }

  public static void broadcastPartyMessage(Component message, Party party) {
    PGM.get().getChannelManager().teamChannel.broadcastMessage(message, party);
  }

  public static void broadcastPartyMessage(
      Component message, Party party, Predicate<MatchPlayer> filter) {
    PGM.get().getChannelManager().teamChannel.broadcastMessage(message, party, filter);
  }
}
