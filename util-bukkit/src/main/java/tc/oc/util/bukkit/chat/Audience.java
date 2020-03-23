package tc.oc.util.bukkit.chat;

import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.util.bukkit.component.Component;
import tc.oc.util.bukkit.component.types.PersonalizedText;

/** Receiver of chat messages, sounds, titles, and other media. */
public interface Audience {

  /**
   * Send a chat message.
   *
   * @param message A message.
   */
  void sendMessage(net.kyori.text.Component message);

  /**
   * Send a warning chat message, with an audio cue.
   *
   * @param message A message.
   */
  void sendWarning(net.kyori.text.Component message);

  /**
   * Send a message above their hotbar.
   *
   * @param message A message.
   */
  void showHotbar(net.kyori.text.Component message);

  /**
   * Send a message as their boss bar, with a progress meter.
   *
   * @param message A message.
   * @param progress The progress of the bar, represented as [0, 1].
   */
  void showBossbar(net.kyori.text.Component message, float progress);

  /**
   * Play a {@link Sound}, by the raw asset name.
   *
   * @param sound The sound.
   */
  void playSound(Sound sound);

  ///////////////////////////////////////////////////////////////
  // METHODS BELOW ARE ALL DEPRECATED AND WILL BE REMOVED SOON //
  ///////////////////////////////////////////////////////////////

  @Deprecated
  void sendMessage(Component message);

  @Deprecated
  default void sendWarning(Component message, boolean audible) {
    sendMessage(
        new PersonalizedText(ChatColor.RED)
            .extra(new PersonalizedText(" \u26a0 ", ChatColor.YELLOW), message));
    if (audible) {
      playSound(new Sound("note.bass", 1f, 0.75f));
    }
  }

  @Deprecated
  default void sendWarning(Component message) {
    sendWarning(message, false);
  }

  @Deprecated
  void sendHotbarMessage(Component message);

  void showTitle(
      @Nullable Component title,
      @Nullable Component subtitle,
      int inTicks,
      int stayTicks,
      int outTicks);

  @Deprecated
  void sendMessage(String message);

  @Deprecated
  default void sendWarning(String message, boolean audible) {
    sendMessage(ChatColor.YELLOW + " \u26a0 " + ChatColor.RED + message);
  }

  @Deprecated
  static Audience get(CommandSender sender) {
    if (sender instanceof Player) {
      return (PlayerAudience) () -> (Player) sender;
    } else {
      return (VirtualAudience) () -> sender;
    }
  }
}
