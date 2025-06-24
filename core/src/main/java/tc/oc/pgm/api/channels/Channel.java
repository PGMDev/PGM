package tc.oc.pgm.api.channels;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.Players;

/**
 * Represents a communication channel for handling chat messages
 *
 * @param <T> the type of the target associated with the channel
 */
public interface Channel<T> {

  CloudKey<String> MESSAGE_KEY = CloudKey.of("message", String.class);

  /**
   * Gets the display name of the channel, defaulting to the first alias.
   *
   * @return the channel's display name
   */
  default String getDisplayName() {
    return getAliases().getFirst();
  }

  /**
   * Retrieves the list of aliases for the channel.
   *
   * @return list of channel aliases
   */
  List<String> getAliases();

  /**
   * A character used as a shortcut prefix in messages to specify the target channel. For instance,
   * a `!` at the start of a message may indicate global channel messaging, or {@code null} if none.
   *
   * @return the shortcut character
   */
  @Nullable
  default Character getShortcut() {
    return null;
  }

  /**
   * Retrieves the channel's setting value, or {@code null} if none.
   *
   * @return the setting value
   */
  default SettingValue getSetting() {
    return null;
  }

  /**
   * Retrieves the format these channel messages should be logged using via the
   * {@code AsyncPlayerChatEvent}.
   *
   * @param target the message target
   * @return formatted messaged printed to console
   */
  default String getLoggerFormat(T target) {
    return "<%s>: %s";
  }

  /**
   * If the channel supports message redirection, where messages may be forwarded to another channel
   * or location e.g. team messages might be redirected to global chat post match end.
   *
   * @return {@code true} if redirection is supported, {@code false} otherwise
   */
  default boolean supportsRedirect() {
    return false;
  }

  /**
   * Checks if a player has permission to send a message in the channel.
   *
   * @param sender the player sending the message
   * @return {@code true} if the player has permission
   */
  default boolean canSendMessage(MatchPlayer sender) {
    return true;
  }

  /**
   * Retrieves the channel target based on the sender and command context.
   *
   * @param sender the sender player
   * @param arguments command arguments
   * @return the target of the channel
   */
  T getTarget(MatchPlayer sender, CommandContext<CommandSender> arguments);

  /**
   * Gets the collection of players viewing messages in this channel for the given target.
   *
   * @param target the message target
   * @return collection of viewers
   */
  Collection<MatchPlayer> getViewers(T target);

  /**
   * Gets the players viewing broadcast messages for the given target.
   *
   * @param target the target audience
   * @return collection of broadcast viewers
   */
  default Collection<MatchPlayer> getBroadcastViewers(T target) {
    return getViewers(target);
  }

  /**
   * Called when a message is sent; allows for additional actions (e.g., sound feedback).
   *
   * @param event the message event
   */
  default void messageSent(final ChannelMessageEvent<T> event) {}

  /**
   * Formats a message for this channel.
   *
   * @param target message target
   * @param sender the sender player (optional)
   * @param message the content to format
   * @return the formatted message
   */
  Component formatMessage(T target, @Nullable MatchPlayer sender, Component message);

  /**
   * Registers commands using all channel aliases by default unless overrode.
   *
   * @param manager command manager for command registration
   */
  default void registerCommand(CommandManager<CommandSender> manager) {
    List<String> aliases = getAliases();
    if (aliases.isEmpty()) return;

    manager.command(manager
        .commandBuilder(aliases.getFirst(), aliases.subList(1, aliases.size()), CommandMeta.empty())
        .optional(
            MESSAGE_KEY,
            StringParser.greedyStringParser(),
            SuggestionProvider.blockingStrings(Players::suggestPlayers))
        .handler(context -> {
          MatchPlayer sender =
              context.inject(MatchPlayer.class).orElseThrow(IllegalStateException::new);

          if (context.contains(MESSAGE_KEY)) {
            PGM.get().getChatManager().process(this, sender, context);
          } else {
            PGM.get().getChatManager().setChannel(sender, this);
          }
        }));
  }

  /**
   * Processes a player's chat message in this channel from a {@code AsyncPlayerChatEvent}. Adding
   * any context required to the command context store.
   *
   * @param sender the player
   * @param message the message content
   * @param context the command context
   */
  default void processChatMessage(
      MatchPlayer sender, String message, CommandContext<CommandSender> context) {
    context.store(MESSAGE_KEY, message);
  }

  /**
   * Processes a chat message from a {@code AsyncPlayerChatEvent} with a shortcut character. Adding
   * any context required to the command context store.
   *
   * @param sender the player
   * @param message the message starting with a shortcut
   * @param context the command context
   */
  default void processChatShortcut(
      MatchPlayer sender, String message, CommandContext<CommandSender> context) {
    if (message.length() == 1) {
      PGM.get().getChatManager().setChannel(sender, this);
      return;
    }

    context.store(Channel.MESSAGE_KEY, message.substring(1).trim());
  }

  /**
   * Broadcasts a message to all viewers of the target.
   *
   * @param component the message to broadcast
   * @param target the target audience
   */
  default void broadcastMessage(Component component, T target) {
    broadcastMessage(component, target, player -> true);
  }

  /**
   * Broadcasts a message to viewers, filtered by a predicate.
   *
   * @param component the message to broadcast
   * @param target the target audience
   * @param filter predicate to filter recipients
   */
  default void broadcastMessage(Component component, T target, Predicate<MatchPlayer> filter) {
    Collection<MatchPlayer> viewers = getBroadcastViewers(target);
    Component finalMessage = formatMessage(target, null, component);
    viewers.stream().filter(filter).forEach(player -> player.sendMessage(finalMessage));
    Audience.console().sendMessage(finalMessage);
  }
}
