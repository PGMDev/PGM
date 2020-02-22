package tc.oc.pgm.api.chat;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;

/**
 * Receiver of chat messages, sounds, titles, and other media.
 *
 * <p>If representing multiple {@link Audience}s, use {@link MultiAudience} instead.
 */
public interface Audience {

  /**
   * Send a message to the {@link Audience}.
   *
   * @param message The message to send.
   */
  void sendMessage(Component message);

  /**
   * Send a warning chat message, with an optional audio cue.
   *
   * @param message The message to send.
   * @param audible Whether to play an audio cue.
   */
  default void sendWarning(Component message, boolean audible) {
    sendMessage(
        new PersonalizedText(ChatColor.RED)
            .extra(new PersonalizedText(" \u26a0 ", ChatColor.YELLOW), message));
    if (audible) {
      playSound(new Sound("note.bass", 1f, 0.75f));
    }
  }

  /**
   * Send a warning chat message.
   *
   * @param message The message to send.
   */
  default void sendWarning(Component message) {
    sendWarning(message, false);
  }

  /**
   * Play a {@link Sound}, by the raw asset name, at the {@link Audience}s location.
   *
   * @param sound The {@link Sound} to play.
   */
  void playSound(Sound sound);

  /**
   * Send a message above the hotbar of the {@link Audience}.
   *
   * @param message The message to send.
   */
  void sendHotbarMessage(Component message);

  /**
   * Show a title, with an optional subtitle, to the {@link Audience}.
   *
   * <p>To clear the title or subtitle of an {@link Audience}, use {@code null}.
   *
   * @param title The title to send, or {@code null} to clear.
   * @param subtitle The subtitle to send, or {@code null} to clear.
   * @param inTicks The number of ticks to fade in the message.
   * @param stayTicks The number of ticks for the message to stay.
   * @param outTicks The number of ticks for the message to fade out.
   */
  void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks);

  /** @see #sendMessage(Component) */
  @Deprecated
  void sendMessage(String message);

  /** @see #sendWarning(Component, boolean) */
  @Deprecated
  default void sendWarning(String message, boolean audible) {
    sendMessage(ChatColor.YELLOW + " \u26a0 " + ChatColor.RED + message);
  }

  @Deprecated
  static Audience get(CommandSender sender) {
    if (sender instanceof Player) {
      return new PlayerAudience((Player) sender);
    } else if (sender instanceof ConsoleCommandSender) {
      return new CommandSenderAudience(sender.getServer().getConsoleSender());
    } else {
      return new CommandSenderAudience(sender);
    }
  }
}
